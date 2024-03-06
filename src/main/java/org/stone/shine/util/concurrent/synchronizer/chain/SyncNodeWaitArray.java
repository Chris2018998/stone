/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer.chain;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater.casPrev;

/**
 * A synchronization node chain(FIFO,applied in result wait pool)
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SyncNodeWaitArray implements Queue<SyncNode> {
    public final SyncNode[] array;
    private volatile int putIndex = -1;
    private volatile int pollIndex = -1;

    public SyncNodeWaitArray(int size) {
        this.array = new SyncNode[size];
    }

    //****************************************************************************************************************//
    //                                          1: queue methods(4)                                                   //
    //****************************************************************************************************************//
    public final SyncNode peek() {
        return array[putIndex];
    }

    public boolean add(SyncNode e) {
        return offer(e);
    }

    public final SyncNode poll() {
        //@todo
        return null;
    }

    public final boolean offer(SyncNode node) {
        return false;
    }

    //****************************************************************************************************************//
    //                                          2: collection methods(3)                                              //
    //****************************************************************************************************************//
    public final boolean remove(Object n) {
        return false;
    }

    public Iterator<SyncNode> iterator() {
        return new SyncNodeWaitArray.DescItr();
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

        DescItr() {
        }

        private static SyncNode findPevNode(SyncNode startNode) {
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

        public boolean hasNext() {
            return false;
            //SyncNode nextNode = findPevNode(curNode);
            //return this.hasPrev = nextNode != null;
        }

        public void remove() {

        }

        public SyncNode next() {
            return null;
//            SyncNode nextNode = findPevNode(curNode);
//            if (nextNode == null && this.hasPrev) throw new ConcurrentModificationException();
//
//            if (curNode == nextNode)
//                curNode = curNode.prev;
//            else
//                this.curNode = nextNode;
//            return nextNode;
        }
    }
}
