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
        return new AscItr(head);
    }

    final Iterator descendingIterator() {
        return new DescItr(tail);
    }

    //****************************************************************************************************************//
    //                                          Iterator implement                                                    //
    //****************************************************************************************************************//
    private static class AscItr implements Iterator {
        private CasNode currentNode;
        private Object currentNodeState;

        public AscItr(CasNode currentNode) {
            this.currentNode = currentNode;
        }

        public boolean hasNext() {
            if (currentNode == null) throw new NoSuchElementException();
            return true;
        }

        public Object next() {
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

    }

    private static class DescItr implements Iterator {
        private CasNode currentNode;

        public DescItr(CasNode currentNode) {
            this.currentNode = currentNode;
        }

        public boolean hasNext() {
            if (currentNode == null) throw new NoSuchElementException();
            return true;
        }

        public Object next() {
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

    }
}