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
            t = tail;//tail always exists
            if (casTail(this, t, node)) {//set as new tail
                t.setNext(node);
                node.setPrev(t);
                return;
            }
        } while (true);
    }

    final boolean remove(CasNode node) {
        CasNode preNode = node.getPrev();
        CasNode predNext = preNode.getNext();

        if (node == tail && casTail(this, node, preNode)) {//node is tail
            casNext(preNode, predNext, null);
            node.setState(REMOVED);
            return true;
        } else if (casNext(preNode, node, node.getNext())) {
            node.setState(REMOVED);
            return true;
        }
        return false;
    }

    final Iterator iterator() {
        return new AscItr(head.getNext());
    }

    final Iterator descendingIterator() {
        return new DescItr(tail);
    }

    //****************************************************************************************************************//
    //                                          Iterator implement                                                    //
    //****************************************************************************************************************//
    private static abstract class PointerItr implements Iterator {
        final ChainPointer point;

        PointerItr(CasNode currentNode) {
            this.point = new ChainPointer(currentNode);
        }

        public boolean hasNext() {
            if (point.currentNode == null) throw new NoSuchElementException();
            if (point.currentState != REMOVED) return true;

            searchNextNode(point);
            if (point.currentNode == null) throw new NoSuchElementException();
            return true;
        }

        public Object next() {
            //1:check current node and item
            CasNode curNode = point.currentNode;
            if (curNode == null) throw new NoSuchElementException();
            Object item = curNode.getState();
            if (item != REMOVED) return item;//valid node

            //2:re-get a valid node from chain
            searchNextNode(point);
            if (point.currentNode == null) throw new ConcurrentModificationException();
            return point.currentState;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        abstract void searchNextNode(ChainPointer point);
    }


    private static class AscItr extends PointerItr {
        AscItr(CasNode currentNode) {
            super(currentNode);
        }

        public void searchNextNode(ChainPointer point) {

        }
    }

    private static class DescItr extends PointerItr {
        DescItr(CasNode currentNode) {
            super(currentNode);
        }

        public void searchNextNode(ChainPointer point) {

        }
    }

    private static class ChainPointer {
        private CasNode currentNode;
        private Object currentState;

        ChainPointer(CasNode currentNode) {
            if (currentNode != null) {
                this.currentNode = currentNode;
                this.currentState = currentNode.getState();
            }
        }

        void fill(CasNode node, Object state) {
            this.currentNode = node;
            this.currentState = state;
        }
    }
}