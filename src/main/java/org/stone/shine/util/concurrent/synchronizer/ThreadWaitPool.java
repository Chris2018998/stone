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

import static java.util.concurrent.locks.LockSupport.unpark;
import static org.stone.shine.util.concurrent.synchronizer.CasNodeUpdater.casState;
import static org.stone.shine.util.concurrent.synchronizer.CasStaticState.INTERRUPTED;
import static org.stone.shine.util.concurrent.synchronizer.CasStaticState.SIGNAL;
import static org.stone.util.CommonUtil.objectEquals;

/**
 * Base Wait-Wakeup Pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitPool<E> {
    //private final CasNodeChain waitQueue = new CasNodeChain();
    private final ConcurrentLinkedDeque<CasNode> waitQueue = new ConcurrentLinkedDeque<>();//temporary

    //****************************************************************************************************************//
    //                                          1: static Methods(3)                                                  //
    //****************************************************************************************************************//
    private static CasNode wakeupOne(final Iterator<CasNode> iterator, final Object toState, final Object type) {
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (type != null && !objectEquals(type, node.type)) continue;
            if (casState(node, null, toState)) {
                unpark(node.thread);
                return node;
            }
        }
        return null;
    }

    private static int wakeupAll(final Iterator<CasNode> iterator, final Object toState, final Object type) {
        int count = 0;
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (type != null && !objectEquals(type, node.type)) continue;
            if (casState(node, null, toState)) {
                unpark(node.thread);
                count++;
            }
        }
        return count;
    }

    //****************************************************************************************************************//
    //                                          2: queue Methods(8)                                                   //
    //****************************************************************************************************************//
    protected final void appendNode(CasNode node) {
        waitQueue.offer(node);
    }

    protected final boolean removeNode(CasNode node) {
        return waitQueue.remove(node);
    }

    protected final CasNode appendDataNode(Object type, Object value) {
        CasNode node = new CasNode(type, value);
        node.thread = null;
        waitQueue.offer(node);
        return node;
    }

    //****************************************************************************************************************//
    //                                          3: Wakeup All                                                         //
    //****************************************************************************************************************//
    public final int wakeupAll() {
        return wakeupAll(waitQueue.iterator(), SIGNAL, null);
    }

    public final int wakeupAll(Object toState) {
        return wakeupAll(waitQueue.iterator(), toState, null);
    }

    public final int wakeupAll(Object toState, Object byType) {
        return wakeupAll(waitQueue.iterator(), toState, byType);
    }

    //****************************************************************************************************************//
    //                                          4: Wakeup One                                                         //
    //****************************************************************************************************************//
    public final int wakeupOne() {
        return wakeupOne(waitQueue.iterator(), SIGNAL, null) != null ? 1 : 0;
    }

    public final int wakeupOne(Object toState) {
        return wakeupOne(waitQueue.iterator(), toState, null) != null ? 1 : 0;
    }

    public final int wakeupOne(Object toState, Object byType) {
        return wakeupOne(waitQueue.iterator(), toState, byType) != null ? 1 : 0;
    }

    public final int wakeupOne(boolean fromHead, Object toState, Object byType) {
        return wakeupOne(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), toState, byType) != null ? 1 : 0;
    }

    protected final CasNode getWokenUpNode(boolean fromHead, Object toState, Object byType) {
        return wakeupOne(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), toState, byType);
    }

    //****************************************************************************************************************//
    //                                         5: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    protected final Iterator<CasNode> ascendingIterator() {
        return waitQueue.iterator();
    }

    public final boolean hasQueuedPredecessors() {
        CasNode node = waitQueue.peek();
        return node != null && node.thread != Thread.currentThread();
    }

    protected final boolean existsTypeNode(Object nodeType) {
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (nodeType == null || objectEquals(nodeType, node.type)) return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.state == null && node.thread != null) return true;
        }
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.thread == thread) return true;
        }
        return false;
    }

    public final int getQueueLength() {
        int count = 0;
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.state == null) count++;
        }
        return count;
    }

    public final Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.state == null && node.thread != null) threadList.add(node.thread);
        }
        return threadList;
    }

    protected int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null) count++;
        }
        return count;
    }

    protected Collection<Thread> getQueuedThreads(Object nodeType) {
        if (nodeType == null) return getQueuedThreads();

        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (objectEquals(nodeType, node.type) && node.state == null && node.thread != null)
                threadList.add(node.thread);
        }
        return threadList;
    }


    //****************************************************************************************************************//
    //                                         6: Park methods(2)                                                     //
    //****************************************************************************************************************//
    protected final void parkNodeThread(CasNode node, ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        parkNodeThread(node, parker, throwsIE, true);
    }

    protected final void parkNodeThread(CasNode node, ThreadParkSupport parker, boolean throwsIE, boolean wakeupOtherOnIE) throws InterruptedException {
        if (parker.parkUtilInterrupted() && throwsIE) {//not timeout and park interrupted
            if (!casState(node, null, INTERRUPTED)) {
                Object state = node.state;
                if (state != null && wakeupOtherOnIE) this.wakeupOne(state, node.type);
            }

            throw new InterruptedException();
        }
    }
}
