/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.firefly.synchronizer.impl.bak;

import org.jmin.firefly.synchronizer.WaitChain;
import org.jmin.firefly.synchronizer.WaitNode;
import org.jmin.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronizeWaitChain implements WaitChain {
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
            prevOffSet = U.objectFieldOffset(WaitNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(WaitNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(WaitNode.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected transient volatile WaitNode head = new WaitNode(AcquireWaitNodeState.EMPTY);
    protected transient volatile WaitNode tail = new WaitNode(AcquireWaitNodeState.EMPTY);

    //***************************************************************************************************************//
    //                                          2: CAS Methods                                                       //
    //***************************************************************************************************************//
    private static boolean casNodeState(WaitNode node, int expect, int update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    private static boolean casTailNext(WaitNode t, WaitNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    private static boolean casHeadPrev(WaitNode h, WaitNode newPrev) {
        return U.compareAndSwapObject(h, prevOffSet, null, newPrev);
    }

    //******************************************** link to next ******************************************************//
    //link next to target node
    private static void linkNextTo(WaitNode startNode, WaitNode endNode) {
        //startNode.next ----> endNode
        WaitNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        WaitNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //physical remove skip node and link to its next node(if its next is null,then link to it)
    private static void linkNextToSkip(WaitNode startNode, WaitNode skipNode) {
        WaitNode next = skipNode.getNext();
        WaitNode endNode = next != null ? next : skipNode;

        //startNode.next -----> endNode
        WaitNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev  -----> startNode
        WaitNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //******************************************** link to prev ******************************************************//
    //link prev to target node
    private static void linkPrevTo(WaitNode startNode, WaitNode endNode) {
        //startNode.prev ----> endNode
        WaitNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        WaitNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //physical remove skip node and link to its prev node(if its prev is null,then link to it)
    private static void linkPrevToSkip(WaitNode startNode, WaitNode skipNode) {
        WaitNode prev = skipNode.getPrev();
        WaitNode endNode = prev != null ? prev : skipNode;

        //startNode.prev ----> endNode
        WaitNode curPrev = startNode.getPrev();
        if (curPrev != endNode) U.compareAndSwapObject(startNode, prevOffSet, curPrev, endNode);

        //endNode.next ------> startNode
        WaitNode curNext = endNode.getNext();
        if (curNext != startNode) U.compareAndSwapObject(endNode, nextOffSet, curNext, startNode);
    }

    //***************************************** get chain fist node and last node ************************************//
    private WaitNode getFirstNode() {
        WaitNode firstNode = head;//assume head is the first node

        do {
            WaitNode prevNode = firstNode.getPrev();
            if (prevNode == null) break;
            firstNode = prevNode;
        } while (true);

        return firstNode;
    }

    private WaitNode getLastNode() {
        WaitNode lastNode = tail;//assume tail is the last node

        do {
            WaitNode nextNode = lastNode.getNext();
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
        for (WaitNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == AcquireWaitNodeState.WAIT_FOR_EXCLUSIVE || state == AcquireWaitNodeState.WAIT_FOR_SHARE || state == AcquireWaitNodeState.RETRY_ACQUIRE)
                size++;
        }
        return size;
    }

    public int getLength(int state) {
        int size = 0;
        for (WaitNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    public Thread[] getThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (WaitNode node = head.getNext(); node != null; node = node.getNext()) {
            int state = node.getState();
            if (state == AcquireWaitNodeState.WAIT_FOR_EXCLUSIVE || state == AcquireWaitNodeState.WAIT_FOR_SHARE || state == AcquireWaitNodeState.RETRY_ACQUIRE)
                threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    public Thread[] getThreads(int state) {
        List<Thread> threadList = new LinkedList<>();
        for (WaitNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() == state) threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }


    public WaitNode addNode(int state) {
        WaitNode node = new WaitNode(state);
        return addNode(node);
    }

    public WaitNode addNode(int state, WaitNode node) {
        node.setState(state);
        node.setNext(null);
        node.setPrev(null);
        return addNode(node);
    }

    private WaitNode addNode(WaitNode node) {
        WaitNode t;
        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casTailNext(t, node)) {//append to tail.next
                node.setPrev(t);
                this.tail = node;//new tail
                return node;
            }
        } while (true);
    }

    public boolean removeNode(WaitNode node) {
        WaitNode prevNode = null;
        WaitNode segStartNode = null;//segment start node
        boolean find = false, removed = false;
        final WaitNode firstNode = this.getFirstNode();

        //loop Iteration direction: head ---> tail
        for (WaitNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            int state = curNode.getState();
            if (state == AcquireWaitNodeState.EMPTY) {
                if (segStartNode == null) segStartNode = prevNode;//mark as start node of a segment
            } else {
                if (curNode == node) {
                    find = true;
                    //false,logic removed or polled by other thread,whether true or false,the current node has become invalid,so need physical remove from chain
                    removed = casNodeState(curNode, state, AcquireWaitNodeState.EMPTY);//logic remove
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
