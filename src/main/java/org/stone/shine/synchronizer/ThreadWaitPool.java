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

import static org.stone.shine.synchronizer.ThreadNodeState.INTERRUPTED;
import static org.stone.shine.synchronizer.ThreadNodeState.SIGNAL;

/**
 * base wait pool,work in Wait-Wakeup Mode
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitPool {

    //temp util ThreadNodeChain is stable
    private final ConcurrentLinkedDeque<ThreadNode> waitQueue = new ConcurrentLinkedDeque<>();

    //****************************************************************************************************************//
    //                                          1: static Methods(3)                                                  //
    //****************************************************************************************************************//
    protected static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    //wakeup one
    private static ThreadNode wakeupOne(Iterator<ThreadNode> iterator, Object toState, Object nodeType) {
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (nodeType != null && !equals(node.getType(), nodeType)) continue;
            if (ThreadNodeUpdater.casNodeState(node, null, toState)) {
                LockSupport.unpark(node.getThread());
                return node;
            }
        }
        return null;
    }

    //wakeup All
    private static int wakeupAll(Iterator<ThreadNode> iterator, Object toState, Object nodeType) {
        int count = 0;
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (nodeType != null && !equals(node.getType(), nodeType)) continue;
            if (ThreadNodeUpdater.casNodeState(node, null, toState)) {
                LockSupport.unpark(node.getThread());
                count++;
            }
        }
        return count;
    }

    //****************************************************************************************************************//
    //                                          2: queue Methods(8)                                                   //
    //****************************************************************************************************************//
    protected final void appendNode(ThreadNode node) {
        waitQueue.offer(node);
    }

    protected final void removeNode(ThreadNode node) {
        waitQueue.remove(node);
    }

    protected final ThreadNode createWaitNode() {
        return createWaitNode(null);
    }

    protected final ThreadNode createWaitNode(Object type) {
        return new ThreadNode(type);
    }

    protected final ThreadNode appendWaitNode() {
        return appendWaitNode(null);
    }

    protected final ThreadNode appendWaitNode(Object type) {
        ThreadNode node = new ThreadNode(type);
        waitQueue.offer(node);
        return node;
    }

    protected final ThreadNode createDataNode(Object type, Object value) {
        return new ThreadNode(type, value);
    }

    protected final ThreadNode appendDataNode(Object type, Object value) {
        ThreadNode node = new ThreadNode(type, value);
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

    public final ThreadNode getWokenUpNode(boolean fromHead, Object toState, Object byType) {
        return wakeupOne(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), toState, byType);
    }

    //****************************************************************************************************************//
    //                                         5: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
    public Iterator<ThreadNode> iterator() {
        return waitQueue.iterator();
    }

    public Iterator<ThreadNode> descendingIterator() {
        return waitQueue.descendingIterator();
    }

    public boolean hasQueuedThreads() {
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (node.getState() == null) return true;
        }
        return false;
    }

    public boolean hasQueuedThread(Thread thread) {
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (node.getThread() == thread) return true;
        }
        return false;
    }

    public int getQueueLength() {
        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (node.getState() == null) count++;
        }
        return count;
    }

    public Collection<Thread> getQueuedThreads() {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (node.getState() == null) threadList.add(node.getThread());
        }
        return threadList;
    }

    protected int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (equals(node.getType(), nodeType) && node.getState() == null) count++;
        }
        return count;
    }

    protected Collection<Thread> getQueuedThreads(Object nodeType) {
        if (nodeType == null) return getQueuedThreads();

        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (equals(node.getType(), nodeType) && node.getState() == null) threadList.add(node.getThread());
        }
        return threadList;
    }


    //****************************************************************************************************************//
    //                                         6: Park methods(2)                                                     //
    //****************************************************************************************************************//
    protected final void parkNodeThread(ThreadNode node, ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        parkNodeThread(node, parker, throwsIE, true);
    }

    protected final void parkNodeThread(ThreadNode node, ThreadParkSupport parker, boolean throwsIE, boolean wakeupOtherOnIE) throws InterruptedException {
        if (parker.calculateParkTime()) {//before deadline
            if (parker.park() && throwsIE) {//interrupted
                if (node.getState() == null) {
                    //step1:try to cas state to INTERRUPTED,if failed,the step2 can be reach
                    if (ThreadNodeUpdater.casNodeState(node, null, INTERRUPTED))
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
