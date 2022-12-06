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

import static org.stone.shine.synchronizer.CasNodeUpdater.casState;

/**
 * A synchronize implementation base class
 *
 * @author Chris Liao
 * @version 1.0
 */

class CasNodeChain {
    protected transient volatile CasNode head = new CasNode(null, null);
    protected transient volatile CasNode tail = new CasNode(null, null);

    public CasNodeChain() {
        this.head.setNext(tail);
        this.tail.setPrev(head);
    }

    public CasNode offer(CasNode node) {
        CasNode t;
        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casState(t, null, node)) {//append to tail.next
                node.setPrev(t);
                this.tail = node;//new tail
                return node;
            }
        } while (true);
    }

//    //remove node from chain(not remove head and tail @todo)
//    public boolean remove(CasNode node) {
//        if (logicRemove(node)) {//logic remove firstly
//            unlinkFromChain(node);
//            return true;
//        }
//        return false;
//    }
//
//    //poll the valid node from chain
//    public CasNode poll() {
//        CasNode prevNode = null;
//        final CasNode firstNode = this.getFirstNode();
//        for (CasNode curNode = firstNode; curNode != null; prevNode = curNode, curNode = curNode.getNext()) {
//            if (logicRemove(curNode)) {//logic remove
//                unlinkFromChain(curNode);
//                return curNode;
//            }
//        }//loop for
//
//        return null;
//    }

    //get number of state node
    public int getLength(Object state) {
        int size = 0;
        for (CasNode node = head.getNext(); node != null; node = node.getNext()) {
            if (state == node.getState())
                size++;
        }
        return size;
    }

    //get runnable of state node
    public Thread[] getThreads(Object state) {
        List<Thread> threadList = new LinkedList<>();
        for (CasNode node = head.getNext(); node != null; node = node.getNext()) {
            if (node.getState() == state) threadList.add(node.getThread());
        }
        return (Thread[]) threadList.toArray();
    }

    private CasNode getFirstNode() {
        CasNode firstNode = head;//assume head is the first node
        do {
            CasNode prevNode = firstNode.getPrev();
            if (prevNode == null) break;
            firstNode = prevNode;
        } while (true);

        return firstNode;
    }
}