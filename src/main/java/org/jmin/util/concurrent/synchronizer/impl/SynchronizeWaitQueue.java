/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl;

import org.jmin.util.concurrent.UnsafeUtil;
import org.jmin.util.concurrent.synchronizer.SynchronizeNode;
import org.jmin.util.concurrent.synchronizer.SynchronizeNodeChain;
import sun.misc.Unsafe;

import java.util.LinkedList;
import java.util.List;

import static org.jmin.util.concurrent.synchronizer.SynchronizeNodeState.*;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronizeWaitQueue implements SynchronizeNodeChain {
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
            prevOffSet = U.objectFieldOffset(SynchronizeNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(SynchronizeNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(SynchronizeNode.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected transient volatile SynchronizeNode head = new SynchronizeNode(EMPTY);
    protected transient volatile SynchronizeNode tail = new SynchronizeNode(EMPTY);

    //***************************************************************************************************************//
    //                                          2: CAS Methods                                                       //
    //***************************************************************************************************************//
    private static boolean casNodeState(SynchronizeNode node, int expect, int update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    private static boolean casTailNext(SynchronizeNode t, SynchronizeNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    private static boolean casHeadPrev(SynchronizeNode h, SynchronizeNode newPrev) {
        return U.compareAndSwapObject(h, prevOffSet, null, newPrev);
    }

    //******************************************** link to next ******************************************************//
    //link next to target node
    private static void linkNextTo(SynchronizeNode startNode, SynchronizeNode endNode) {
        //startNode.next ----> endNode
        SynchronizeNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        SynchronizeNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //physical remove skip node and link to its next node(if its next is null,then link to it)
    private static void linkNextToSkip(SynchronizeNode startNode, SynchronizeNode skipNode) {
        SynchronizeNode next = skipNode.getNext();
        SynchronizeNode endNode = next != null ? next : skipNode;

        //startNode.next -----> endNode
        SynchronizeNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev  -----> startNode
        SynchronizeNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //******************************************** link to prev ******************************************************//
    //link prev to target node
    private static void linkPrevTo(SynchronizeNode startNode, SynchronizeNode endNode) {
        //startNode.prev ----> endNode
        SynchronizeNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        SynchronizeNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //physical remove skip node and link to its prev node(if its prev is null,then link to it)
    private static void linkPrevToSkip(SynchronizeNode startNode, SynchronizeNode skipNode) {
        SynchronizeNode prev = skipNode.getPrev();
        SynchronizeNode endNode = prev != null ? prev : skipNode;

        //startNode.prev ----> endNode
        SynchronizeNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        SynchronizeNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //***************************************** get chain fist node and last node ************************************//
    private SynchronizeNode getFirstNode() {
        SynchronizeNode firstNode = head;//assume head is the first node

        do {
            SynchronizeNode prevNode = firstNode.getPrev();
            if (prevNode == null) break;
            firstNode = prevNode;
        } while (true);

        return firstNode;
    }

    private SynchronizeNode getLastNode() {
        SynchronizeNode lastNode = tail;//assume tail is the last node

        do {
            SynchronizeNode nextNode = lastNode.getNext();
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
        for (SynchronizeNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == WAIT_FOR_EXCLUSIVE || state == WAIT_FOR_SHARE || state == RETRY_ACQUIRE)
                size++;
        }
        return size;
    }

    public int getLength(int state) {
        int size = 0;
        for (SynchronizeNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    public Thread[] getThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (SynchronizeNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == WAIT_FOR_EXCLUSIVE || state == WAIT_FOR_SHARE || state == RETRY_ACQUIRE)
                threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    public Thread[] getThreads(int state) {
        List<Thread> threadList = new LinkedList<>();
        for (SynchronizeNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() == state) threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }


    public SynchronizeNode addNode(int state) {
        SynchronizeNode node = new SynchronizeNode(state);
        return addNode(node);
    }

    public SynchronizeNode addNode(int state, SynchronizeNode node) {
        node.setState(state);
        node.setNext(null);
        node.setPrev(null);
        return addNode(node);
    }

    private SynchronizeNode addNode(SynchronizeNode node) {
        SynchronizeNode t;
        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casTailNext(t, node)) {//append to tail.next
                node.setPrev(t);
                this.tail = node;//new tail
                return node;
            }
        } while (true);
    }

    public boolean removeNode(SynchronizeNode node) {
        SynchronizeNode prevNode = null;
        SynchronizeNode segStartNode = null;//segment start node
        boolean find = false, removed = false;
        final SynchronizeNode firstNode = this.getFirstNode();

        //loop Iteration direction: head ---> tail
        for (SynchronizeNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            int state = curNode.getState();
            if (state == EMPTY) {
                if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
            } else {
                if (curNode == node) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = casNodeState(curNode, state, EMPTY);//logic remove
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
