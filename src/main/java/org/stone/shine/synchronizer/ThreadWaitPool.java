/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998(cn),All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.synchronizer.CasNodeState.INTERRUPTED;
import static org.stone.shine.synchronizer.CasNodeState.SIGNAL;

/**
 * Base Wait-Wakeup Pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitPool<E> {

    //temp util ThreadNodeChain is stable
    private final ConcurrentLinkedDeque<CasNode> waitQueue = new ConcurrentLinkedDeque<>();

    //****************************************************************************************************************//
    //                                          1: static Methods(3)                                                  //
    //****************************************************************************************************************//
    protected static boolean equals(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    //wakeup one
    private static CasNode wakeupOne(Iterator<CasNode> iterator, Object toState, Object nodeType) {
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (nodeType != null && !equals(nodeType, node.getType())) continue;
            if (CasNodeUpdater.casNodeState(node, null, toState)) {
                LockSupport.unpark(node.getThread());
                return node;
            }
        }
        return null;
    }

    //wakeup All
    private static int wakeupAll(Iterator<CasNode> iterator, Object toState, Object nodeType) {
        int count = 0;
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (nodeType != null && !equals(nodeType, node.getType())) continue;
            if (CasNodeUpdater.casNodeState(node, null, toState)) {
                LockSupport.unpark(node.getThread());
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
        node.setThread(null);
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

    public final CasNode getWokenUpNode(boolean fromHead, Object toState, Object byType) {
        return wakeupOne(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), toState, byType);
    }

    //****************************************************************************************************************//
    //                                         5: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    public final Iterator<CasNode> ascendingIterator() {
        return waitQueue.iterator();
    }

    public final Iterator<CasNode> descendingIterator() {
        return waitQueue.descendingIterator();
    }

    protected boolean existsTypeNode(Object nodeType) {
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (equals(nodeType, node.getType())) return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.getState() == null && node.getThread() != null) return true;
        }
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.getThread() == thread) return true;
        }
        return false;
    }

    public final int getQueueLength() {
        int count = 0;
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.getState() == null) count++;
        }
        return count;
    }

    public final Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (node.getState() == null && node.getThread() != null) threadList.add(node.getThread());
        }
        return threadList;
    }

    protected int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (equals(nodeType, node.getType()) && node.getState() == null) count++;
        }
        return count;
    }

    protected Collection<Thread> getQueuedThreads(Object nodeType) {
        if (nodeType == null) return getQueuedThreads();

        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<CasNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            CasNode node = iterator.next();
            if (equals(nodeType, node.getType()) && node.getState() == null && node.getThread() != null)
                threadList.add(node.getThread());
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
        if (parker.calculateParkTime()) {//before deadline
            if (parker.park() && throwsIE) {//interrupted
                if (node.getState() == null) {
                    //step1:try to cas state to INTERRUPTED,if failed,the step2 can be reach
                    if (CasNodeUpdater.casNodeState(node, null, INTERRUPTED))
                        throw new InterruptedException();
                }

                //step2:send signal state to other when got
                Object state = node.getState();
                if (state != null && wakeupOtherOnIE)
                    wakeupOne(state);//send the got signal state to another waiter(skip over the current node during wakeup iterator)
                throw new InterruptedException();
            }
        }
    }
}
