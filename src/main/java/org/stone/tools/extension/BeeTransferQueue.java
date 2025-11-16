/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.extension;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.stone.tools.extension.BeeTransferQueueNode.NULL;

/**
 * {@link #BeeTransferQueue} is a customization queue implementation.
 * <p>
 * Class Logic copied from OPEN-JDK-25,Thank Doug Lee and all experts of Java concurrent package.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeTransferQueue implements BeeInterruptable {
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle NEXT;
    private static final VarHandle ITEM;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(BeeTransferQueue.class, "head", BeeTransferQueueNode.class);
            TAIL = l.findVarHandle(BeeTransferQueue.class, "tail", BeeTransferQueueNode.class);

            ITEM = l.findVarHandle(BeeTransferQueueNode.class, "item", Object.class);
            NEXT = l.findVarHandle(BeeTransferQueueNode.class, "next", BeeTransferQueueNode.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    //chain head
    private transient volatile BeeTransferQueueNode head;
    //Tail node of chain
    private transient volatile BeeTransferQueueNode tail;

    //constructor to create head node
    public BeeTransferQueue() {
        this.tail = this.head = new BeeTransferQueueNode();
    }

    /**
     * Clear All nodes
     */
    public void clear() {
        this.tail = this.head;
    }

    /**
     * Check exist waiters in chain
     *
     * @return true if {@code tail.item !=REMOVED}
     */
    public boolean existWaiters() {
        return tail.item != NULL;
    }

    /**
     * Offers specified node to chain.
     *
     * @param newNode to be offered
     * @return true when success
     */
    public boolean offer(BeeTransferQueueNode newNode) {// logic copy from OPEN-JDK
        for (BeeTransferQueueNode t = tail, p = t; ; ) {
            BeeTransferQueueNode q = p.next;
            if (q == null) {
                if (NEXT.compareAndSet(p, null, newNode)) {
                    TAIL.weakCompareAndSet(this, t, newNode);
                    return true;
                }
            } else if (p == q)
                p = (t != (t = tail)) ? t : head;
            else
                p = (p != t && t != (t = tail)) ? t : q;
        }
    }

    /**
     * Removes given node from chain.
     *
     * @param node to be removed
     * @return true when success
     */
    public boolean remove(BeeTransferQueueNode node) {
        //1: set removed flag
        node.item = NULL;//removed flag,borrower threads offer their nodes and remove them

        restartFromHead:
        do {
            for (BeeTransferQueueNode p = head, pred = null; p != null; ) {
                BeeTransferQueueNode q = p.next;
                if (p == node) {
                    skipDeadNodes(pred, p, p, q);
                    return true;
                } else if (p.item != NULL) {
                    pred = p;
                    p = q;
                } else {
                    for (BeeTransferQueueNode c = p; ; q = p.next) {
                        if (q == null || q.item != NULL) {
                            pred = skipDeadNodes(pred, c, p, q);
                            p = q;
                            break;
                        }
                        if (p == (p = q)) continue restartFromHead;
                    }
                }
            }
            return false;
        } while (true);
    }

    private boolean tryCasSuccessor(BeeTransferQueueNode pred, BeeTransferQueueNode c, BeeTransferQueueNode p) {
        if (pred != null)
            return NEXT.compareAndSet(pred, c, p);
        if (HEAD.compareAndSet(this, c, p)) {
            NEXT.setRelease(c, c);
            return true;
        }
        return false;
    }

    private BeeTransferQueueNode skipDeadNodes(BeeTransferQueueNode pred, BeeTransferQueueNode c, BeeTransferQueueNode p, BeeTransferQueueNode q) {
        if (q == null) {
            if (c == p) return pred;
            q = p;
        }
        return (tryCasSuccessor(pred, c, q)
                && (pred == null || ITEM.get(pred) != NULL))
                ? pred : p;
    }
    
    public boolean isEmpty() {
        return first() == null;
    }

    BeeTransferQueueNode first() {
        restartFromHead:
        for (; ; ) {
            for (BeeTransferQueueNode h = head, p = h, q; ; p = q) {
                boolean hasItem = (p.item != NULL);
                if (hasItem || (q = p.next) == null) {
                    updateHead(h, p);
                    return hasItem ? p : null;
                } else if (p == q)
                    continue restartFromHead;
            }
        }
    }

    void updateHead(BeeTransferQueueNode h, BeeTransferQueueNode p) {
        if (h != p && HEAD.compareAndSet(this, h, p))
            NEXT.setRelease(h, h);
    }

    /**
     * ** Key Method **:Attempt to transfer given value object to waiter in queue.
     *
     * @param expect   is cas expected value
     * @param newValue to be transferred
     * @return true when success
     */
    public boolean tryTransfer(Object expect, Object newValue) {//need to locate the first node in chain.
        for (BeeTransferQueueNode p = head.next; p != null; ) {
            if (p.item == NULL) continue;
            if (ITEM.compareAndSet(p, expect, newValue)) {
                LockSupport.unpark(p.thread);
                return true;
            } else {
                p = p.next;
            }
        }
        return false;
    }

    //****************************************************************************************************************//
    //                                              Waiting threads                                                   //
    //****************************************************************************************************************//
    public List<Thread> getQueuedThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (BeeTransferQueueNode p = head.next; p != null; p = p.next) {
            if (p.item != NULL) threadList.add(p.thread);
        }
        return threadList;
    }

    public List<Thread> interruptQueuedWaitThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (BeeTransferQueueNode p = head.next; p != null; p = p.next) {
            if (p.item != NULL) {
                p.thread.interrupt();
                threadList.add(p.thread);
            }
        }
        return threadList;
    }
}