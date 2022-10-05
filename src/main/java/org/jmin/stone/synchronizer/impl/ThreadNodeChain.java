/*
 * Copyright(C) Chris2018998
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

    static {
        try {
            U = UnsafeUtil.getUnsafe();
            prevOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected transient volatile ThreadNode head = new ThreadNode(ThreadNodeState.EMPTY);
    protected transient volatile ThreadNode tail = new ThreadNode(ThreadNodeState.EMPTY);

    //***************************************************************************************************************//
    //                                          2: CAS Methods                                                       //
    //***************************************************************************************************************//
    static boolean casNodeState(ThreadNode node, int expect, int update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    private static boolean casTailNext(ThreadNode t, ThreadNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    //link cas node next to new node
    private static void linkNextTo(ThreadNode casNode, ThreadNode next, boolean skipNext) {
        ThreadNode newNext;
        if (skipNext) {
            newNext = next.getNext();
            if (newNext == null) newNext = next;
        } else {
            newNext = next;
        }

        //casNode.next ----> newNext
        ThreadNode curNext = casNode.getNext();
        if (curNext != newNext) U.compareAndSwapObject(casNode, nextOffSet, curNext, newNext);

        //newNext.prev ------> casNode
        ThreadNode curPrev = newNext.getPrev();
        if (curPrev != casNode) U.compareAndSwapObject(newNext, prevOffSet, curPrev, casNode);
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

    //***************************************************************************************************************//
    //                                          4: Interface Methods                                                 //
    //***************************************************************************************************************//

    public int getLength() {
        int size = 0;
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == ThreadNodeState.WAITING || state == ThreadNodeState.ACQUIRE)
                size++;
        }
        return size;
    }

    public int getLength(int state) {
        int size = 0;
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    public Thread[] getThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == ThreadNodeState.WAITING || state == ThreadNodeState.ACQUIRE)
                threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    public Thread[] getThreads(int state) {
        List<Thread> threadList = new LinkedList<>();
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() == state) threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    public ThreadNode addNode(int state) {
        ThreadNode node = new ThreadNode(state);
        return addNode(node);
    }

    public ThreadNode addNode(int state, long type) {
        ThreadNode node = new ThreadNode(state);
        return addNode(node);
    }

    public ThreadNode addNode(int state, ThreadNode node) {
        node.setState(state);
        node.setNext(null);
        node.setPrev(null);
        return addNode(node);
    }

    public ThreadNode addNode(ThreadNode node) {
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

    public boolean removeNode(ThreadNode node) {
        ThreadNode prevNode = null;
        ThreadNode segStartNode = null;//segment start node
        boolean find = false, removed = false;
        final ThreadNode firstNode = this.getFirstNode();

        //loop Iteration direction: head ---> tail
        for (ThreadNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            int state = curNode.getState();
            if (state == ThreadNodeState.EMPTY) {
                if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
            } else {
                if (curNode == node) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = casNodeState(curNode, state, ThreadNodeState.EMPTY);//logic remove
                }

                if (segStartNode != null) {//end a segment
                    if (find) {
                        linkNextTo(segStartNode, curNode, true);//link to current node 'next
                        return removed;
                    } else
                        linkNextTo(segStartNode, curNode, false);//link to current node
                    segStartNode = null;
                } else if (find) {//preNode is a valid node
                    if (prevNode != null) linkNextTo(prevNode, curNode, true);
                    return removed;
                }
            }
        }//loop

        if (segStartNode != null) linkNextTo(segStartNode, prevNode, true);
        return false;
    }

    /**
     * Retrieves and removes the first element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    public ThreadNode pollFirst() {
        ThreadNode prevNode = null;
        final ThreadNode firstNode = this.getFirstNode();
        for (ThreadNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            int state = curNode.getState();
            if (state != ThreadNodeState.EMPTY && casNodeState(curNode, state, ThreadNodeState.EMPTY)) {//failed means the node has removed by other thread
                linkNextTo(firstNode, curNode, true);
                return curNode;
            }
        }//loop for

        if (prevNode != null) linkNextTo(firstNode, prevNode, true);
        return null;
    }
}
