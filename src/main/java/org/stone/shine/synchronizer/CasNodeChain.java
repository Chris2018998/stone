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
    private final CasNode head = new CasNode(REMOVED);
    private volatile CasNode tail = head;

    final void offer(CasNode node) {
        CasNode t;
        do {
            t = this.tail;
        } while (!casTail(this, t, node));

        t.setNext(node);
        node.setPrev(t);
    }

    final boolean remove(CasNode node) {
        node.setState(REMOVED);
        CasNode preNode = node.getPrev();
        if (node == this.tail && casTail(this, node, preNode)) {
            casNext(preNode, preNode.getNext(), null);
        } else {
            CasNode nodeNext = node.getNext();
            if (casNext(preNode, preNode.getNext(), nodeNext) && nodeNext != null) {
                casPrev(nodeNext, nodeNext.getPrev(), preNode);
            }
        }
        return true;
    }

    final Iterator iterator() {
        return new AscItr(head.getNext());
    }

    final Iterator descendingIterator() {
        CasNode t = tail;
        return new DescItr(t != head ? t : null);
    }

    //****************************************************************************************************************//
    //                                          Iterator implement                                                    //
    //****************************************************************************************************************//
    private static abstract class PointerItr implements Iterator {
        private final ChainPointer pointer;

        PointerItr(CasNode currentNode) {
            this.pointer = new ChainPointer(currentNode);
            if (pointer.currentNode != null && pointer.currentState == REMOVED)
                searchNode(pointer);
        }

        public boolean hasNext() {
            if (pointer.currentNode == null) return false;
            searchNode(pointer);
            return pointer.currentNode != null;
        }

        public Object next() {
            //1:check current node and item
            if (pointer.currentNode == null) throw new NoSuchElementException();

            //2:re-get a valid node from chain
            searchNode(pointer);
            if (pointer.currentNode == null) throw new ConcurrentModificationException();
            return pointer.currentState;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        abstract void searchNode(ChainPointer point);
    }

    private static class AscItr extends PointerItr {
        AscItr(CasNode currentNode) {
            super(currentNode);
        }

        public void searchNode(ChainPointer pointer) {
            CasNode searchedNode = null;
            Object searchedNodeState = null;
            CasNode curNode = pointer.currentNode.getNext();

            while (curNode != null) {
                Object state = curNode.getState();
                if (state != REMOVED) {//find a valid node
                    searchedNode = curNode;
                    searchedNodeState = state;
                    break;
                }
                curNode = curNode.getNext();
            }

            pointer.fill(searchedNode, searchedNodeState);
        }
    }

    private static class DescItr extends PointerItr {
        DescItr(CasNode currentNode) {
            super(currentNode);
        }

        public void searchNode(ChainPointer pointer) {
            CasNode searchedNode = null;
            Object searchedNodeState = null;
            CasNode curNode = pointer.currentNode.getPrev();

            while (curNode != null) {
                Object state = curNode.getState();
                if (state != REMOVED) {//find a valid node
                    searchedNode = curNode;
                    searchedNodeState = state;
                    break;
                }
                curNode = curNode.getPrev();
            }

            pointer.fill(searchedNode, searchedNodeState);
        }
    }

    private static class ChainPointer {
        private CasNode curNode;
        private boolean existNextNode;
        private CasNode targetNextNode;
        private Object targetNextState;

        public ChainPointer(CasNode curNode) {
            this.curNode = curNode;
        }

        public CasNode getCurNode() {
            return curNode;
        }

        public boolean existNextNode() {
            return existNextNode;
        }

        public CasNode getNextNode() {
            return targetNextNode;
        }

        public Object getNextNodeState() {
            return targetNextState;
        }

        public void fillNextNode(boolean existNextNode, CasNode nextNode, Object nextNodeState) {
            this.existNextNode = existNextNode;
            this.targetNextNode = nextNode;
            this.targetNextState = nextNodeState;
        }
    }
}