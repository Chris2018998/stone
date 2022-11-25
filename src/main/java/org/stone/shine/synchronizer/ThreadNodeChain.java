/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer;

import java.util.LinkedList;
import java.util.List;

import static org.stone.shine.synchronizer.ThreadNodeUpdater.*;

/**
 * A synchronize implementation base class
 *
 * @author Chris Liao
 * @version 1.0
 */

class ThreadNodeChain {
    protected transient volatile ThreadNode head = new ThreadNode(null);
    protected transient volatile ThreadNode tail = new ThreadNode(null);

    public ThreadNodeChain() {
        this.head.setEmptyInd(1);
        this.tail.setEmptyInd(1);
        this.head.setNext(tail);
        this.tail.setPrev(head);
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
    public int getLength(Object state) {
        int size = 0;
        for (ThreadNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    //get runnable of state node
    public Thread[] getThreads(Object state) {
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