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

import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater.*;

/**
 * A synchronization node chain(FIFO,applied in result wait pool)
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class SyncNodeChain implements Queue<SyncNode> {
    private SyncNode head = new SyncNode(null);
    volatile SyncNode tail = head;

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
        SyncNode pred = node.prev;
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

        private SyncNode findPevNode(SyncNode startNode) {
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
    }
}