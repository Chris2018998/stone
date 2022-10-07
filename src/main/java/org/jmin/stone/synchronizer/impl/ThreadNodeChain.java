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
    private final static long valueOffSet;

    static {
        try {
            U = UnsafeUtil.getUnsafe();
            prevOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("prev"));
            nextOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("next"));
            stateOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("state"));
            valueOffSet = U.objectFieldOffset(ThreadNode.class.getDeclaredField("value"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    protected transient volatile ThreadNode head = new ThreadNode();
    protected transient volatile ThreadNode tail = new ThreadNode();

    //***************************************************************************************************************//
    //                                          2: CAS Methods                                                       //
    //***************************************************************************************************************//
    static boolean casNodeState(ThreadNode node, int expect, int update) {
        return U.compareAndSwapObject(node, stateOffSet, expect, update);
    }

    static boolean casNodeValue(ThreadNode node, Object expect, Object update) {
        return U.compareAndSwapObject(node, valueOffSet, expect, update);
    }

    private static boolean casTailNext(ThreadNode t, ThreadNode newNext) {
        return U.compareAndSwapObject(t, nextOffSet, null, newNext);
    }

    //******************************************** link to next ******************************************************//
    //physical remove skip node and link to its next node(if its next is null,then link to it)
    private static void skipNextTo(ThreadNode startNode, ThreadNode skipNode) {
        ThreadNode next = skipNode.getNext();
        linkNextTo(startNode, next != null ? next : skipNode);
    }

    //link next to target node
    private static void linkNextTo(ThreadNode startNode, ThreadNode endNode) {
        //startNode.next ----> endNode
        ThreadNode curNext = startNode.getNext();
        if (curNext != endNode) U.compareAndSwapObject(startNode, nextOffSet, curNext, endNode);

        //endNode.prev ------> startNode
        ThreadNode curPrev = endNode.getPrev();
        if (curPrev != startNode) U.compareAndSwapObject(endNode, prevOffSet, curPrev, startNode);
    }

    //***************************************************************************************************************//
    //                                          4: Interface Methods                                                 //
    //***************************************************************************************************************//

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

    //remove node from chain
    public boolean remove(ThreadNode node) {
        Object value = node.getValue();
        if (value != null && casNodeValue(node, value, null)) {
            ThreadNode prevNode = node.getPrev();
            skipNextTo(prevNode, node);
            return true;
        }
        return false;
    }

    public ThreadNode poll() {
        ThreadNode prevNode = null;
        final ThreadNode firstNode = this.getFirstNode();
        for (ThreadNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
            Object value = curNode.getValue();
            if (value != null && casNodeValue(curNode, value, null)) {
                skipNextTo(firstNode, curNode);
                return curNode;
            }
        }//loop for

        if (prevNode != null) skipNextTo(firstNode, prevNode);
        return null;
    }

    public int getLength(int state) {
        int size = 0;
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

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