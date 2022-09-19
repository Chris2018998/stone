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
final class SynchronizedNodeChain {
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
            prevOffSet = U.objectFieldOffset(SynchronizedNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(SynchronizedNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(SynchronizedNode.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected transient volatile SynchronizedNode head = new SynchronizedNode(SynchronizedNodeState.EMPTY);
    protected transient volatile SynchronizedNode tail = new SynchronizedNode(SynchronizedNodeState.EMPTY);

    //***************************************************************************************************************//
    //                                          2: CAS Methods                                                       //
    //***************************************************************************************************************//
    private static boolean casNodeState(SynchronizedNode node, int expect, int update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    private static boolean casTailNext(SynchronizedNode t, SynchronizedNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    private static boolean casHeadPrev(SynchronizedNode h, SynchronizedNode newPrev) {
        return U.compareAndSwapObject(h, prevOffSet, null, newPrev);
    }

    //******************************************** link to next ******************************************************//
    //link next to target node
    private static void linkNextTo(SynchronizedNode startNode, SynchronizedNode endNode) {
        //startNode.next ----> endNode
        SynchronizedNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        SynchronizedNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //physical remove skip node and link to its next node(if its next is null,then link to it)
    private static void linkNextToSkip(SynchronizedNode startNode, SynchronizedNode skipNode) {
        SynchronizedNode next = skipNode.getNext();
        SynchronizedNode endNode = next != null ? next : skipNode;

        //startNode.next -----> endNode
        SynchronizedNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev  -----> startNode
        SynchronizedNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //******************************************** link to prev ******************************************************//
    //link prev to target node
    private static void linkPrevTo(SynchronizedNode startNode, SynchronizedNode endNode) {
        //startNode.prev ----> endNode
        SynchronizedNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        SynchronizedNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //physical remove skip node and link to its prev node(if its prev is null,then link to it)
    private static void linkPrevToSkip(SynchronizedNode startNode, SynchronizedNode skipNode) {
        SynchronizedNode prev = skipNode.getPrev();
        SynchronizedNode endNode = prev != null ? prev : skipNode;

        //startNode.prev ----> endNode
        SynchronizedNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        SynchronizedNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //***************************************** get chain fist node and last node ************************************//
    private SynchronizedNode getFirstNode() {
        SynchronizedNode firstNode = head;//assume head is the first node

        do {
            SynchronizedNode prevNode = firstNode.getPrev();
            if (prevNode == null) break;
            firstNode = prevNode;
        } while (true);

        return firstNode;
    }

    private SynchronizedNode getLastNode() {
        SynchronizedNode lastNode = tail;//assume tail is the last node

        do {
            SynchronizedNode nextNode = lastNode.getNext();
            if (nextNode == null) break;
            lastNode = nextNode;
        } while (true);

        return lastNode;
    }

    //***************************************************************************************************************//
    //                                          4: Interface Methods                                                 //
    //***************************************************************************************************************//

    public int getLength() {
        int size = 0;
        for (SynchronizedNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == SynchronizedNodeState.WAIT_EXCLUSIVE || state == SynchronizedNodeState.WAIT_SHARED || state == SynchronizedNodeState.ACQUIRE_RETRY)
                size++;
        }
        return size;
    }

    public int getLength(int state) {
        int size = 0;
        for (SynchronizedNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    public Thread[] getThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (SynchronizedNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == SynchronizedNodeState.WAIT_EXCLUSIVE || state == SynchronizedNodeState.WAIT_SHARED || state == SynchronizedNodeState.ACQUIRE_RETRY)
                threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    public Thread[] getThreads(int state) {
        List<Thread> threadList = new LinkedList<>();
        for (SynchronizedNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() == state) threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }


    public SynchronizedNode addNode(int state) {
        SynchronizedNode node = new SynchronizedNode(state);
        return addNode(node);
    }

    public SynchronizedNode addNode(int state, SynchronizedNode node) {
        node.setState(state);
        node.setNext(null);
        node.setPrev(null);
        return addNode(node);
    }

    public SynchronizedNode addNode(SynchronizedNode node) {
        SynchronizedNode t;
        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casTailNext(t, node)) {//append to tail.next
                node.setPrev(t);
                this.tail = node;//new tail
                return node;
            }
        } while (true);
    }

    public boolean removeNode(SynchronizedNode node) {
        SynchronizedNode prevNode = null;
        SynchronizedNode segStartNode = null;//segment start node
        boolean find = false, removed = false;
        final SynchronizedNode firstNode = this.getFirstNode();

        //loop Iteration direction: head ---> tail
        for (SynchronizedNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            int state = curNode.getState();
            if (state == SynchronizedNodeState.EMPTY) {
                if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
            } else {
                if (curNode == node) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = casNodeState(curNode, state, SynchronizedNodeState.EMPTY);//logic remove
                }

                if (segStartNode != null) {//end a segment
                    if (find) {
                        linkNextToSkip(segStartNode, curNode);//link to current node 'next
                        return removed;
                    } else
                        linkNextTo(segStartNode, curNode);//link to current node
                    segStartNode = null;
                } else if (find) {//preNode is a valid node
                    if (prevNode != null) linkNextToSkip(prevNode, curNode);
                    return removed;
                }
            }
        }//loop

        if (segStartNode != null) linkNextToSkip(segStartNode, prevNode);
        return false;
    }
}
