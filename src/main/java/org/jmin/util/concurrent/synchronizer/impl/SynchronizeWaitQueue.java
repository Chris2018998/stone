/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl;

import org.jmin.util.concurrent.UnsafeUtil;
import org.jmin.util.concurrent.synchronizer.PermitWaitNode;
import org.jmin.util.concurrent.synchronizer.PermitWaitNodeChain;
import sun.misc.Unsafe;

import java.util.LinkedList;
import java.util.List;

import static org.jmin.util.concurrent.synchronizer.PermitWaitNodeState.*;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronizeWaitQueue implements PermitWaitNodeChain {
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
            prevOffSet = U.objectFieldOffset(PermitWaitNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(PermitWaitNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(PermitWaitNode.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected transient volatile PermitWaitNode head = new PermitWaitNode(EMPTY);
    protected transient volatile PermitWaitNode tail = new PermitWaitNode(EMPTY);

    //***************************************************************************************************************//
    //                                          2: CAS Methods                                                       //
    //***************************************************************************************************************//
    private static boolean casNodeState(PermitWaitNode node, int expect, int update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    private static boolean casTailNext(PermitWaitNode t, PermitWaitNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    private static boolean casHeadPrev(PermitWaitNode h, PermitWaitNode newPrev) {
        return U.compareAndSwapObject(h, prevOffSet, null, newPrev);
    }

    //******************************************** link to next ******************************************************//
    //link next to target node
    private static void linkNextTo(PermitWaitNode startNode, PermitWaitNode endNode) {
        //startNode.next ----> endNode
        PermitWaitNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        PermitWaitNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //physical remove skip node and link to its next node(if its next is null,then link to it)
    private static void linkNextToSkip(PermitWaitNode startNode, PermitWaitNode skipNode) {
        PermitWaitNode next = skipNode.getNext();
        PermitWaitNode endNode = next != null ? next : skipNode;

        //startNode.next -----> endNode
        PermitWaitNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev  -----> startNode
        PermitWaitNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //******************************************** link to prev ******************************************************//
    //link prev to target node
    private static void linkPrevTo(PermitWaitNode startNode, PermitWaitNode endNode) {
        //startNode.prev ----> endNode
        PermitWaitNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        PermitWaitNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //physical remove skip node and link to its prev node(if its prev is null,then link to it)
    private static void linkPrevToSkip(PermitWaitNode startNode, PermitWaitNode skipNode) {
        PermitWaitNode prev = skipNode.getPrev();
        PermitWaitNode endNode = prev != null ? prev : skipNode;

        //startNode.prev ----> endNode
        PermitWaitNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        PermitWaitNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //***************************************** get chain fist node and last node ************************************//
    private PermitWaitNode getFirstNode() {
        PermitWaitNode firstNode = head;//assume head is the first node

        do {
            PermitWaitNode prevNode = firstNode.getPrev();
            if (prevNode == null) break;
            firstNode = prevNode;
        } while (true);

        return firstNode;
    }

    private PermitWaitNode getLastNode() {
        PermitWaitNode lastNode = tail;//assume tail is the last node

        do {
            PermitWaitNode nextNode = lastNode.getNext();
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
        for (PermitWaitNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == WAIT_FOR_EXCLUSIVE || state == WAIT_FOR_SHARE || state == RETRY_ACQUIRE)
                size++;
        }
        return size;
    }

    public int getLength(int state) {
        int size = 0;
        for (PermitWaitNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    public Thread[] getThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (PermitWaitNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == WAIT_FOR_EXCLUSIVE || state == WAIT_FOR_SHARE || state == RETRY_ACQUIRE)
                threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    public Thread[] getThreads(int state) {
        List<Thread> threadList = new LinkedList<>();
        for (PermitWaitNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() == state) threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }


    public PermitWaitNode addNode(int state) {
        PermitWaitNode node = new PermitWaitNode(state);
        return addNode(node);
    }

    public PermitWaitNode addNode(int state, PermitWaitNode node) {
        node.setState(state);
        node.setNext(null);
        node.setPrev(null);
        return addNode(node);
    }

    private PermitWaitNode addNode(PermitWaitNode node) {
        PermitWaitNode t;
        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casTailNext(t, node)) {//append to tail.next
                node.setPrev(t);
                this.tail = node;//new tail
                return node;
            }
        } while (true);
    }

    public boolean removeNode(PermitWaitNode node) {
        PermitWaitNode prevNode = null;
        PermitWaitNode segStartNode = null;//segment start node
        boolean find = false, removed = false;
        final PermitWaitNode firstNode = this.getFirstNode();

        //loop Iteration direction: head ---> tail
        for (PermitWaitNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
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
