/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.util.LinkedList;
import java.util.List;

/**
 * A synchronize implementation base class
 *
 * @author Chris Liao
 * @version 1.0
 */

class ThreadNodeChain {
    //***************************************************************************************************************//
    //                                           1: CAS Chain info                                                   //
    //***************************************************************************************************************//
    private final static Unsafe U;
    private final static long prevOffSet;
    private final static long nextOffSet;
    private final static long stateOffSet;
    private final static long removedIndOffSet;

    static {
        try {
            U = UnsafeUtil.getUnsafe();
            prevOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("state"));
            removedIndOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("emptyInd"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected transient volatile ThreadNode head = new ThreadNode();
    protected transient volatile ThreadNode tail = new ThreadNode();

    //***************************************************************************************************************//
    //                                          4: Interface Methods                                                 //
    //***************************************************************************************************************//
    public ThreadNodeChain() {
        this.head.setEmptyInd(1);
        this.tail.setEmptyInd(1);
        this.head.setNext(tail);
        this.tail.setPrev(head);
    }

    //****************************************************************************************************************//
    //                                          2: CAS methods                                                        //
    //****************************************************************************************************************//
    static boolean logicRemove(ThreadNode node) {
        return U.compareAndSwapObject(node, removedIndOffSet, 0, 1);
    }

    static boolean casNodeState(ThreadNode node, int expect, int update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    private static boolean casTailNext(ThreadNode t, ThreadNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    private static void unlinkFromChain(ThreadNode node) {
        ThreadNode prev = node.getPrev();
        ThreadNode next = node.getNext();
        if (prev != null && next != null) linkNextTo(prev, next);

    }

    private static void linkNextTo(ThreadNode startNode, ThreadNode endNode) {
        //startNode.next ----> endNode
        ThreadNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        ThreadNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    public ThreadNode offer(ThreadNode node) {
        ThreadNode t;
        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casTailNext(t, node)) {//append to tail.next
                node.setPrev(t);
                this.tail = node;//new tail
                return node;
            }
        } while (true);
    }

    //remove node from chain(not remove head and tail @todo)
    public boolean remove(ThreadNode node) {
        if (logicRemove(node)) {//logic remove firstly
            unlinkFromChain(node);
            return true;
        }
        return false;
    }

    //poll the valid node from chain
    public ThreadNode poll() {
        ThreadNode prevNode = null;
        final ThreadNode firstNode = this.getFirstNode();
        for (ThreadNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            if (logicRemove(curNode)) {//logic remove
                unlinkFromChain(curNode);
                return curNode;
            }
        }//loop for

        return null;
    }

    //get number of state node
    public int getLength(int state) {
        int size = 0;
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    //get threads of state node
    public Thread[] getThreads(int state) {
        List<Thread> threadList = new LinkedList<>();
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() == state) threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    private ThreadNode getFirstNode() {
        ThreadNode firstNode = head;//assume head is the first node
        do {
            ThreadNode prevNode = firstNode.getPrev();
            if (prevNode == null) break;
            firstNode = prevNode;
        } while (true);

        return firstNode;
    }
}