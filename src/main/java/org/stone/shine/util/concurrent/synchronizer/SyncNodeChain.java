/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

import java.util.*;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casNext;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casTail;

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
    public SyncNode peek() {
        return head.next;
    }

    public boolean add(SyncNode e) {
        return offer(e);
    }

    public SyncNode poll() {
        SyncNode node = head.next;
        if (node != null) {
            head = node;
            node.thread = null;
            node.prev = null;
        }
        return node;
    }

    public boolean offer(SyncNode node) {
        SyncNode t;
        do {
            t = tail;
            node.prev = t;
            if (casTail(this, t, node)) {//append to tail.next
                t.next = node;
                return true;
            }
        } while (true);
    }

    public SyncNode remove() {
        throw new UnsupportedOperationException();
    }

    public SyncNode element() {
        throw new UnsupportedOperationException();
    }

    //****************************************************************************************************************//
    //                                          2: collection methods(3)                                              //
    //****************************************************************************************************************//
    public boolean remove(Object n) {
        SyncNode node = (SyncNode) n;

        SyncNode pred = node.prev;
        SyncNode predNext = pred.next;
        if (node == tail && casTail(this, node, pred)) {
            casNext(pred, predNext, null);
            return true;
        } else {
            SyncNode next = node.next;
            casNext(pred, predNext, next);
        }
        return true;
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

    public Iterator<SyncNode> iterator() {
        SyncNode t = tail;
        return new DescItr(t != head ? t : null);
    }

    //****************************************************************************************************************//
    //                                          Iterator implement                                                    //
    //****************************************************************************************************************//
    private static abstract class PointerItr implements Iterator<SyncNode> {
        private final ChainPointer pointer;

        PointerItr(SyncNode currentNode) {
            this.pointer = new ChainPointer(currentNode);
        }

        //valid node exists test method
        public boolean hasNext() {
            if (pointer.curNode == null) return false;

            //try to search a valid node after/prev current node(test)
            findNextNode(pointer);
            return pointer.nextNode != null;
        }

        public SyncNode next() {
            //1:check current node and item
            if (pointer.curNode == null) throw new NoSuchElementException();

            //2:retry to find a valid node start at current node
            findNextNode(pointer);

            SyncNode nextNode = pointer.nextNode;
            if (nextNode == null) throw new ConcurrentModificationException();
            pointer.movePointerToNext();
            return nextNode;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        //fill a valid node to the
        abstract void findNextNode(ChainPointer pointer);
    }

    private static class DescItr extends PointerItr {
        DescItr(SyncNode currentNode) {
            super(currentNode);
        }

        public void findNextNode(ChainPointer pointer) {
            SyncNode curNode = pointer.curNode;
            SyncNode nextNode = pointer.isAtFirst() ? curNode : curNode.prev;

            while (nextNode != null) {
                if (nextNode.state != SyncNodeStates.REMOVED) {//find a valid node
                    pointer.setNextNode(nextNode);
                    break;
                }
                nextNode = nextNode.prev;
            }
        }
    }

    private static class ChainPointer {
        private final SyncNode firstNode;
        private SyncNode curNode;
        private SyncNode nextNode;

        ChainPointer(SyncNode firstNode) {
            this.firstNode = firstNode;
            this.curNode = firstNode;
        }

        boolean isAtFirst() {
            return curNode == firstNode;
        }

        void setNextNode(SyncNode nextNode) {
            this.nextNode = nextNode;
        }

        void movePointerToNext() {
            this.curNode = nextNode;
            this.nextNode = null;
        }
    }
}