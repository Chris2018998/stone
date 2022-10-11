/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.LinkedList;
import java.util.List;

import static org.jmin.stone.synchronizer.impl.ThreadNodeUpdater.*;

/**
 * A synchronize implementation base class
 *
 * @author Chris Liao
 * @version 1.0
 */

class ThreadNodeChain {
    protected transient volatile ThreadNode head = new ThreadNode();
    protected transient volatile ThreadNode tail = new ThreadNode();

    //**************************************************************************************************************//
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