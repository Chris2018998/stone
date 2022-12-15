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

import static org.stone.shine.synchronizer.CasNodeUpdater.casState;
import static org.stone.shine.synchronizer.CasNodeUpdater.casTail;

/**
 * A synchronize node chain
 *
 * @author Chris Liao
 * @version 1.0
 */

final class CasNodeChain {
    private final CasNode head = new CasNode(null);
    private volatile CasNode tail = head;

    public final boolean offer(CasNode node) {
        CasNode t;
        do {
            t = tail;//tail always exists
            if (casTail(this, t, node)) {//set as new tail
                t.setNext(node);
                node.setPrev(t);
                return true;
            }
        } while (true);
    }

    public final boolean remove(CasNode node) {
        CasNode t;
        do {
            t = tail;//tail always exists
            if (t.getNext() == null && casState(t, null, node)) {//append to tail.next
                node.setPrev(t);
                this.tail = node;//new tail
                return true;
            }
        } while (t != null);
        return false;
    }

    public final Iterator<CasNode> iterator() {
        return null;
    }

    public final Iterator<CasNode> descendingIterator() {
        return null;
    }
}