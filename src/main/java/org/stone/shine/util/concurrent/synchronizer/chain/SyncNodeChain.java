/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.chain;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Queue;

import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeStates.REMOVED;
import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater.*;

/**
 * A synchronization node chain(FIFO,applied in result wait pool)
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class SyncNodeChain implements Queue<SyncNode> {
    private SyncNode head = new SyncNode(null);//dummy node at first()
    volatile SyncNode tail = head;

    public static void main(String[] ags) {
        SyncNode node1 = new SyncNode<>(11, 111);
        SyncNode node2 = new SyncNode<>(22, 222);
        SyncNode node3 = new SyncNode<>(33, 333);
        SyncNodeChain chain = new SyncNodeChain();
        chain.offer(node1);
        chain.offer(node2);
        chain.offer(node3);

        //remove
        node2.setState(REMOVED);
        chain.remove(node2);
        node3.setState(REMOVED);
        chain.remove(node3);

        SyncNode node = chain.head;
        while (true) {
            SyncNode next = node.getNext();
            if (next != null) {
                System.out.println("Node:" + next);
                node = next;
            } else {
                break;
            }
        }
    }

    //****************************************************************************************************************//
    //                                          1: queue methods(4)                                                   //
    //****************************************************************************************************************//
    public final SyncNode peek() {
        return head.next;
    }

    public boolean add(SyncNode e) {
        return offer(e);
    }

    public final SyncNode poll() {
        SyncNode node = head.next;
        if (node != null) {
            node.thread = null;
            node.prev = null;
            head = node;
        }
        return node;
    }

    public final boolean offer(SyncNode node) {
        do {
            SyncNode t = tail;
            node.prev = t;
            if (casTail(this, t, node)) {
                t.next = node;
                return true;
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          2: collection methods(3)                                              //
    //****************************************************************************************************************//
    public final boolean remove(Object n) {
        SyncNode node = (SyncNode) n;
        node.thread = null;
        node.state = REMOVED;

        //1: find not removed node
        SyncNode pred = node.prev;
        while (pred.state == REMOVED)
            node.prev = pred = pred.prev;

        //2: remove from chain
        SyncNode predNext = pred.next;
        if (node == tail && casTail(this, node, pred)) {
            casNext(pred, predNext, null);
            return true;
        } else {
            return casNext(pred, predNext, node.next);
        }
    }

    public Iterator<SyncNode> iterator() {
        return new DescItr(tail);
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends SyncNode> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    public SyncNode remove() {
        throw new UnsupportedOperationException();
    }

    public SyncNode element() {
        throw new UnsupportedOperationException();
    }

    //****************************************************************************************************************//
    //                                          Iterator implement                                                    //
    //****************************************************************************************************************//
    private static class DescItr implements Iterator<SyncNode> {
        private SyncNode curNode;
        private boolean hasPrev;

        DescItr(SyncNode curNode) {
            this.curNode = curNode;
        }

        private static SyncNode findPevNode(SyncNode startNode) {
            SyncNode targetNode = null;
            SyncNode curNode = startNode;

            while (curNode != null) {
                if (curNode.thread != null)
                    targetNode = curNode;
                if (curNode != startNode)
                    casPrev(startNode, startNode.prev, curNode);
                if (targetNode != null) break;

                curNode = curNode.prev;
            }
            return targetNode;
        }

        public boolean hasNext() {
            SyncNode nextNode = findPevNode(curNode);
            return this.hasPrev = nextNode != null;
        }

        public SyncNode next() {
            SyncNode nextNode = findPevNode(curNode);
            if (nextNode == null && this.hasPrev) throw new ConcurrentModificationException();

            if (curNode == nextNode)
                curNode = curNode.prev;
            else
                this.curNode = nextNode;
            return nextNode;
        }
    }
}