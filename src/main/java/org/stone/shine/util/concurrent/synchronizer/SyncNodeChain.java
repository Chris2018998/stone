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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A synchronization node chain
 *
 * @author Chris Liao
 * @version 1.0
 */
//final class SyncNodeChain<SyncNode> implements Deque<SyncNode> {
final class SyncNodeChain {
    private final SyncNode head = new SyncNode(null);
    volatile SyncNode tail = head;

    SyncNodeChain() {
        head.thread = null;
    }

    final void offer(SyncNode node) {
        SyncNode t;
        do {
            t = tail;
            node.prev = t;
            if (SyncNodeUpdater.casTail(this, t, node)) {//append to tail.next
                t.next = node;
                return;
            }
        } while (true);
    }

    final boolean remove(SyncNode node) {
        node.setState(SyncNodeStates.REMOVED);

        //1: find out not removed pre-node
        SyncNode pred = node.prev;
        while (pred.state == SyncNodeStates.REMOVED)
            node.prev = pred = pred.prev;

        //2:remove node from chain
        SyncNode predNext = pred.next;
        if (node == tail && SyncNodeUpdater.casTail(this, node, pred)) {
            SyncNodeUpdater.casNext(pred, predNext, null);
        } else {
            SyncNode next = node.next;
            SyncNodeUpdater.casNext(pred, predNext, next);
        }
        return true;
    }

    final Iterator<SyncNode> iterator() {
        return new AscItr(head.next);
    }

    final Iterator<SyncNode> descendingIterator() {
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

    private static class AscItr extends PointerItr {
        AscItr(SyncNode currentNode) {
            super(currentNode);
        }

        public void findNextNode(ChainPointer pointer) {
            SyncNode curNode = pointer.curNode;
            SyncNode nextNode = pointer.isAtFirst() ? curNode : curNode.next;

            while (nextNode != null) {
                if (nextNode.state != SyncNodeStates.REMOVED) {//find a valid node
                    pointer.setNextNode(nextNode);
                    break;
                }
                nextNode = curNode.next;
            }
        }
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