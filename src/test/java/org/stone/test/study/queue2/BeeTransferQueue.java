/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.study.queue2;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * {@link #BeeTransferQueue} is a customization queue.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeTransferQueue implements BeeInterruptable {
    //Special Value of node marked as deleted status
    private static final Object REMOVED = new Object();
    private static final VarHandle NEXT;
    private static final VarHandle ITEM;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            ITEM = l.findVarHandle(BeeTransferQueueNode.class, "item", Object.class);
            NEXT = l.findVarHandle(BeeTransferQueueNode.class, "next", BeeTransferQueueNode.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    //Not movable
    private final BeeTransferQueueNode head;
    //Tail node of chain
    private volatile BeeTransferQueueNode tail;

    //constructor to create head node
    public BeeTransferQueue() {
        this.tail = this.head = new BeeTransferQueueNode(null, REMOVED);
    }

    /**
     * Offers specified node to chain.
     *
     * @param node to be offered
     * @return true when success
     */
    public boolean offer(BeeTransferQueueNode node) {
        node.item = null;
        do {
            if (NEXT.compareAndSet(this.tail, null, node)) {//append to tail.next
                this.tail = node;
                return true;
            }
        } while (true);
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
        return tail.item != REMOVED;
    }

    /**
     * Remove given node from chain. (End story of a node)
     *
     * @param node to be removed
     * @return true when success
     */
    public boolean remove(BeeTransferQueueNode node) {
        //1: mark as removed status
        node.item = REMOVED;

        //2: get head node as prev node of first node
        BeeTransferQueueNode prevNode = head;
        BeeTransferQueueNode curNode = prevNode.next;

        //3: prev node of first removed flag node of some segment
        BeeTransferQueueNode prevOfFirstDeleted = null;

        //4: loop to search the specified node
        do {
            if (curNode == node) {//OK,found you,abandon you
                BeeTransferQueueNode linkTo = curNode.next;
                if (linkTo != null) {//plan to skip over you,link to your next node
                    if (prevOfFirstDeleted == null) prevOfFirstDeleted = prevNode;
                    NEXT.compareAndSet(prevOfFirstDeleted, prevOfFirstDeleted.next, linkTo);
                } else if (prevOfFirstDeleted != null && prevOfFirstDeleted != prevNode) {
                    BeeTransferQueueNode deletedNext = prevOfFirstDeleted.next;
                    if (deletedNext != prevNode) NEXT.compareAndSet(prevOfFirstDeleted, deletedNext, prevNode);
                }
                return true;
            } else if (curNode.item == REMOVED) {//a deletion node
                if (prevOfFirstDeleted == null) prevOfFirstDeleted = prevNode;
            } else if (prevOfFirstDeleted != null) {//Not removed
                NEXT.compareAndSet(prevOfFirstDeleted, prevOfFirstDeleted.next, curNode);
                prevOfFirstDeleted = null;
            }

            //move to next node
            prevNode = curNode;
            curNode = curNode.next;
            if (curNode == null) return false;
        } while (true);
    }

    /**
     * Find the first undeleted node from chain
     *
     * @return BeeTransferQueueNode when success
     */
    public BeeTransferQueueNode peek() {
        //1: read head and set it as start node
        BeeTransferQueueNode prevNode = head;
        BeeTransferQueueNode curNode = prevNode.next;

        //2: if first node is null,is that the queue is empty
        if (curNode == null) return null;

        //3: prev node of first deletion node of some segment
        BeeTransferQueueNode prevOfFirstDeleted = null;

        //4: loop to search first node not removed
        do {
            if (curNode.item != REMOVED) {//OK,found a node not removed
                if (prevOfFirstDeleted != null) {
                    BeeTransferQueueNode deletedNext = prevOfFirstDeleted.next;
                    if (prevOfFirstDeleted != curNode && deletedNext != curNode)
                        NEXT.weakCompareAndSet(prevOfFirstDeleted, deletedNext, curNode);
                }
                return curNode;
            } else if (prevOfFirstDeleted == null) {
                prevOfFirstDeleted = prevNode;
            }

            //move to next node
            prevNode = curNode;
            curNode = curNode.next;
            if (curNode == null) return null;
        } while (true);

    }

    /**
     * ** Key Method **:Attempt to transfer given value object to waiter in queue.
     *
     * @param value to be transferred
     * @return true when success
     */
    public boolean tryTransfer(Object value) {//need to locate the first node in chain.
        for (BeeTransferQueueNode p = head.next; p != null; ) {
            if (p.item == REMOVED) continue;
            if (ITEM.compareAndSet(p, null, value)) {
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
            if (p.item != REMOVED) threadList.add(p.thread);
        }
        return threadList;
    }

    public List<Thread> interruptQueuedWaitThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (BeeTransferQueueNode p = head.next; p != null; p = p.next) {
            if (p.item != REMOVED) {
                p.thread.interrupt();
                threadList.add(p.thread);
            }
        }
        return threadList;
    }
}
