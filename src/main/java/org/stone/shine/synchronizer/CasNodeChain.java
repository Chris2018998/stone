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
        CasNode preNode = node.prev;
        CasNode nextNode = node.next;

        if (casNext(preNode, node, nextNode)) {
            if (nextNode != null)
                nextNode.prev = preNode;
            else
                this.tail = preNode;
        }
        return true;
    }

    final Iterator iterator() {
        return new AscItr(head.next);
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
        }

        //valid node exists test method
        public boolean hasNext() {
            if (pointer.curNode == null) return false;

            //try to search a valid node after/prev current node(test)
            fillNextNode(pointer);
            return pointer.nextNode != null;
        }

        public Object next() {
            //1:check current node and item
            if (pointer.curNode == null) throw new NoSuchElementException();

            //2:retry to find a valid node start at current node
            fillNextNode(pointer);
            setNewCurrentNode(pointer);
            if (pointer.nextNode == null) throw new ConcurrentModificationException();
            return pointer.nextState;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        //fill a valid node to the
        abstract void fillNextNode(ChainPointer pointer);

        //set searched valid node as new
        abstract void setNewCurrentNode(ChainPointer pointer);
    }

    private static class AscItr extends PointerItr {
        AscItr(CasNode currentNode) {
            super(currentNode);
        }

        //set searched valid node as new
        public void setNewCurrentNode(ChainPointer pointer) {
            CasNode curNode = pointer.curNode;
            CasNode nextNode = pointer.nextNode;
            if (curNode == nextNode) {
                pointer.setCurNode(curNode.next);
            } else {
                pointer.setCurNode(nextNode);
            }
        }

        public void fillNextNode(ChainPointer pointer) {
            CasNode curNode = pointer.curNode;
            CasNode nextNode = pointer.isAtFirst() ? curNode : curNode.next;

            while (nextNode != null) {
                Object state = nextNode.state;
                if (state != REMOVED) {//find a valid node
                    pointer.fillNextNode(nextNode, state);
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

        //set searched valid node as new
        public void setNewCurrentNode(ChainPointer pointer) {
            CasNode curNode = pointer.curNode;
            CasNode nextNode = pointer.nextNode;
            if (curNode == nextNode) {
                pointer.setCurNode(curNode.prev);
            } else {
                pointer.setCurNode(nextNode);
            }
        }

        public void fillNextNode(ChainPointer pointer) {
            CasNode curNode = pointer.curNode;
            CasNode nextNode = pointer.isAtFirst() ? curNode : curNode.prev;

            while (nextNode != null) {
                Object state = nextNode.state;
                if (state != REMOVED) {//find a valid node
                    pointer.fillNextNode(nextNode, state);
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
        private Object nextState;

        ChainPointer(CasNode firstNode) {
            this.firstNode = firstNode;
            this.curNode = firstNode;
        }

        boolean isAtFirst() {
            return curNode == firstNode;
        }

        void setCurNode(CasNode curNode) {
            this.curNode = curNode;
        }

        void fillNextNode(CasNode nextNode, Object nextState) {
            this.nextNode = nextNode;
            this.nextState = nextState;
        }
    }
}