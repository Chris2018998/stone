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

import static org.stone.shine.synchronizer.CasNodeUpdater.*;
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
        CasNode preNextNode = preNode.getNext();

        if (node == tail) {//node is tail
            if (casTail(this, node, preNode)) {
                casNext(preNode, preNextNode, null);
                return true;
            }
        } else {
            CasNode nodeNext = node.getNext();
            CasNode nodePrev = nodeNext.getPrev();
            if (casNext(preNode, preNextNode, nodeNext)) {
                casPrev(nodeNext, nodePrev, preNode);
                return true;
            }
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
        final ChainPointer pointer;

        PointerItr(CasNode currentNode) {
            this.pointer = new ChainPointer(currentNode);
        }

        public boolean hasNext() {
            if (pointer.currentNode == null) return false;
            if (pointer.currentState != REMOVED) return true;

            searchNextNode(pointer);
            return pointer.currentNode != null;
        }

        public Object next() {
            //1:check current node and item
            CasNode curNode = pointer.currentNode;
            if (curNode == null) throw new NoSuchElementException();
            if (pointer.currentState != REMOVED) return pointer.currentState;//valid node

            //2:re-get a valid node from chain
            searchNextNode(pointer);
            if (pointer.currentNode == null) throw new ConcurrentModificationException();
            return pointer.currentState;
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
            CasNode searchedNode = null;
            Object searchedNodeState = null;
            CasNode curNode = pointer.currentNode.getNext();

            try {
                while (curNode != null) {
                    Object state = curNode.getState();
                    if (state != REMOVED) {//find a valid node
                        searchedNode = curNode;
                        searchedNodeState = state;
                        break;
                    }
                    curNode = curNode.getNext();
                }
            } finally {
                pointer.fill(searchedNode, searchedNodeState);
            }
        }
    }

    private static class DescItr extends PointerItr {
        DescItr(CasNode currentNode) {
            super(currentNode);
        }

        public void searchNextNode(ChainPointer point) {
            CasNode searchedNode = null;
            Object searchedNodeState = null;
            CasNode curNode = pointer.currentNode.getPrev();

            try {
                while (curNode != null) {
                    Object state = curNode.getState();
                    if (state != REMOVED) {//find a valid node
                        searchedNode = curNode;
                        searchedNodeState = state;
                        break;
                    }
                    curNode = curNode.getPrev();
                }
            } finally {
                pointer.fill(searchedNode, searchedNodeState);
            }
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