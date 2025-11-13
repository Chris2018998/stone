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

/**
 * {@link #BeeTransferQueue} is a customization queue for stone project to improve pool performance.
 * <p>
 * its implementation is a unique technical of stone project,feature is below
 * 1: This queue chain has a fixed head node,which is not moved when queue method operation
 * 2: Head node and tail node is same when queue instantiated (initial state)
 * 3: Tail node is remained in chain when all nodes are removed(two nodes exists in chain)
 * <p>
 * Note: It is a private tool,Forbidden to copy its logic or apply it in other projects.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeTransferQueue implements BeeInterruptable {
    //Special Value of node marked as deleted status
    public static final Object REMOVED = new Object();
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

    //Head node is not movable node in chain *
    private final BeeTransferQueueNode head;
    //Tail node of chain
    private volatile BeeTransferQueueNode tail;

    //constructor to create head node
    public BeeTransferQueue() {
        this.tail = this.head = new BeeTransferQueueNode(null);
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
            if (NEXT.compareAndSet(tail, null, node)) {//append to tail.next
                this.tail = node;
                return true;
            }
        } while (true);
    }

    /**
     * Remove given node from chain. (End story of a node)
     *
     * @param node to be removed
     * @return true when success
     */
    public boolean remove(BeeTransferQueueNode node) {
        //1: set deleted statue to node
        node.item = REMOVED;

        //2: get first node as a start node
        BeeTransferQueueNode curNode = head.next;
        //3: if first node is null,is that the queue is empty
        if (curNode == null) return false;

        //4: get head node as prev node of first node
        BeeTransferQueueNode prevNode = head;
        //5: prev node of first deletion node of some segment
        BeeTransferQueueNode prevOfFirstDeleted = null;

        //6: loop to search the specified node
        do {
            if (curNode == node) {//OK,found you,abandon you
                BeeTransferQueueNode linkTo = curNode.next;//plan to skip over you,link to your next node
                if (linkTo == null) linkTo = prevNode;//At tail,so lucky,remain you,then link to your prev node.

                if (prevOfFirstDeleted == null) prevOfFirstDeleted = prevNode;
                BeeTransferQueueNode deletedNext = prevOfFirstDeleted.next;
                if (prevOfFirstDeleted != linkTo && deletedNext != linkTo)
                    NEXT.weakCompareAndSet(prevOfFirstDeleted, deletedNext, linkTo);

                return true;
            } else if (curNode.item == REMOVED) {//mark as removed
                if (prevOfFirstDeleted == null) prevOfFirstDeleted = prevNode;
            } else if (prevOfFirstDeleted != null) {//Not deleted
                NEXT.weakCompareAndSet(prevOfFirstDeleted, prevOfFirstDeleted.next, curNode);
                prevOfFirstDeleted = null;
            }

            //move current node to next
            prevNode = curNode;
            curNode = curNode.next;
            if (curNode == null) return false;
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
            threadList.add(p.thread);
        }
        return threadList;
    }

    public List<Thread> interruptQueuedWaitThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (BeeTransferQueueNode p = head.next; p != null; p = p.next) {
            p.thread.interrupt();
            threadList.add(p.thread);
        }
        return threadList;
    }
}
