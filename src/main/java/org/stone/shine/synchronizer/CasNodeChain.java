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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.stone.shine.synchronizer.CasNodeUpdater.casNext;
import static org.stone.shine.synchronizer.CasNodeUpdater.casTail;
import static org.stone.shine.synchronizer.CasStaticState.REMOVED;

/**
 * A synchronize node chain
 *
 * @author Chris Liao
 * @version 1.0
 */

final class CasNodeChain {
    private final CasNode head = new CasNode(null);
    private volatile CasNode tail = head;

    final void offer(CasNode node) {
        CasNode t;
        do {
            t = tail;
            node.prev = t;
            if (casTail(this, t, node)) {//append to tail.next
                t.next = node;
                return;
            }
        } while (true);
    }

    final boolean remove(CasNode node) {
        node.setState(REMOVED);

        //1: find out not removed pre-node
        CasNode pred = node.prev;
        while (pred.state == REMOVED)
            node.prev = pred = pred.prev;

        //2:remove node from chain
        CasNode predNext = pred.next;
        if (node == tail && casTail(this, node, pred)) {
            casNext(pred, predNext, null);
        } else {
            CasNode next = node.next;
            casNext(pred, predNext, next);
        }
        return true;
    }

    final Iterator<CasNode> iterator() {
        return new AscItr(head.next);
    }

    final Iterator<CasNode> descendingIterator() {
        CasNode t = tail;
        return new DescItr(t != head ? t : null);
    }

    //****************************************************************************************************************//
    //                                          Iterator implement                                                    //
    //****************************************************************************************************************//
    private static abstract class PointerItr implements Iterator<CasNode> {
        private final ChainPointer pointer;

        PointerItr(CasNode currentNode) {
            this.pointer = new ChainPointer(currentNode);
        }

        //valid node exists test method
        public boolean hasNext() {
            if (pointer.curNode == null) return false;

            //try to search a valid node after/prev current node(test)
            findNextNode(pointer);
            return pointer.nextNode != null;
        }

        public CasNode next() {
            //1:check current node and item
            if (pointer.curNode == null) throw new NoSuchElementException();

            //2:retry to find a valid node start at current node
            findNextNode(pointer);

            CasNode nextNode = pointer.nextNode;
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
        AscItr(CasNode currentNode) {
            super(currentNode);
        }

        public void findNextNode(ChainPointer pointer) {
            CasNode curNode = pointer.curNode;
            CasNode nextNode = pointer.isAtFirst() ? curNode : curNode.next;

            while (nextNode != null) {
                if (nextNode.state != REMOVED) {//find a valid node
                    pointer.setNextNode(nextNode);
                    break;
                }
                nextNode = curNode.next;
            }
        }
    }

    private static class DescItr extends PointerItr {
        DescItr(CasNode currentNode) {
            super(currentNode);
        }

        public void findNextNode(ChainPointer pointer) {
            CasNode curNode = pointer.curNode;
            CasNode nextNode = pointer.isAtFirst() ? curNode : curNode.prev;

            while (nextNode != null) {
                if (nextNode.state != REMOVED) {//find a valid node
                    pointer.setNextNode(nextNode);
                    break;
                }
                nextNode = nextNode.prev;
            }
        }
    }

    private static class ChainPointer {
        private final CasNode firstNode;
        private CasNode curNode;
        private CasNode nextNode;

        ChainPointer(CasNode firstNode) {
            this.firstNode = firstNode;
            this.curNode = firstNode;
        }

        boolean isAtFirst() {
            return curNode == firstNode;
        }

        void setNextNode(CasNode nextNode) {
            this.nextNode = nextNode;
        }

        void movePointerToNext() {
            this.curNode = nextNode;
            this.nextNode = null;
        }
    }
}