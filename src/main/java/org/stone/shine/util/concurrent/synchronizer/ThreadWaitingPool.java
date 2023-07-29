/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998(cn),All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.stone.tools.CommonUtil.objectEquals;

/**
 * Base Wait-Wakeup Pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitingPool<E> {
    //private final SyncNodeChain waitChain = new SyncNodeChain();
    private final ConcurrentLinkedDeque<SyncNode> waitChain = new ConcurrentLinkedDeque<>();//temporary

    //****************************************************************************************************************//
    //                                          2: queue Methods(8)                                                   //
    //****************************************************************************************************************//
    protected final void appendNode(SyncNode node) {
        waitChain.offer(node);
    }

    protected final boolean removeNode(SyncNode node) {
        return waitChain.remove(node);
    }

    protected final SyncNode appendDataNode(Object state, Object type, E value) {
        SyncNode node = new SyncNode<E>(state, type, value);
        waitChain.offer(node);
        return node;
    }


//    //****************************************************************************************************************//
//    //                                          1: static Methods(3)                                                  //
//    //****************************************************************************************************************//
//    private static SyncNode wakeupOne(final Iterator<SyncNode> iterator, final Object toState, final Object type) {
//        while (iterator.hasNext()) {
//            SyncNode node = iterator.next();
//            if (type != null && !objectEquals(type, node.type)) continue;
//            if (casState(node, null, toState)) {
//                unpark(node.thread);
//                return node;
//            }
//        }
//        return null;
//    }
//
//    private static int wakeupAll(final Iterator<SyncNode> iterator, final Object toState, final Object type) {
//        int count = 0;
//        while (iterator.hasNext()) {
//            SyncNode node = iterator.next();
//            if (type != null && !objectEquals(type, node.type)) continue;
//            if (casState(node, null, toState)) {
//                unpark(node.thread);
//                count++;
//            }
//        }
//        return count;
//    }

//    public final int wakeupAll() {
//        return wakeupAll(waitChain.iterator(), SIGNAL, null);
//    }
//
//    public final int wakeupAll(Object toState) {
//        return wakeupAll(waitChain.iterator(), toState, null);
//    }
//
//    public final int wakeupAll(Object toState, Object byType) {
//        return wakeupAll(waitChain.iterator(), toState, byType);
//    }

    //****************************************************************************************************************//
    //                                          4: Wakeup One                                                         //
    //****************************************************************************************************************//
    public final int wakeupOne() {
        return wakeupOne(waitChain.iterator(), SIGNAL, null) != null ? 1 : 0;
    }

    public final int wakeupOne(Object toState) {
        return wakeupOne(waitChain.iterator(), toState, null) != null ? 1 : 0;
    }

    public final int wakeupOne(Object toState, Object byType) {
        return wakeupOne(waitChain.iterator(), toState, byType) != null ? 1 : 0;
    }

    public final int wakeupOne(boolean fromHead, Object toState, Object byType) {
        return wakeupOne(fromHead ? waitChain.iterator() : waitChain.descendingIterator(), toState, byType) != null ? 1 : 0;
    }

    protected final SyncNode getWokenUpNode(boolean fromHead, Object toState, Object byType) {
        return wakeupOne(fromHead ? waitChain.iterator() : waitChain.descendingIterator(), toState, byType);
    }


    //****************************************************************************************************************//
    //                                         5: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    protected final Iterator<SyncNode> ascendingIterator() {
        return waitChain.iterator();
    }

    public final boolean hasQueuedPredecessors() {
        SyncNode node = waitChain.peek();
        return node != null && node.thread != Thread.currentThread();
    }

    protected final boolean existsTypeNode(Object nodeType) {
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (nodeType == null || objectEquals(nodeType, node.type)) return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null && node.thread != null) return true;
        }
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.thread == thread) return true;
        }
        return false;
    }

    public final int getQueueLength() {
        int count = 0;
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null) count++;
        }
        return count;
    }

    public final Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (node.state == null && node.thread != null) threadList.add(node.thread);
        }
        return threadList;
    }

    protected int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null) count++;
        }
        return count;
    }

    protected Collection<Thread> getQueuedThreads(Object nodeType) {
        if (nodeType == null) return getQueuedThreads();

        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<SyncNode> iterator = waitChain.iterator();
        while (iterator.hasNext()) {
            SyncNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null && node.thread != null)
                threadList.add(node.thread);
        }
        return threadList;
    }
}
