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
    //                                          1: static Methods(2)                                                  //
    //****************************************************************************************************************//
    protected static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    //wake wait node by iterator
    private static int wakeup(Iterator<ThreadNode> iterator, Object toState, ThreadNode skipNode, boolean justOne, Object nodeType) {
        int count = 0;
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (node == skipNode) continue;
            if (nodeType != null && !equals(node.getType(), nodeType)) continue;

            if (ThreadNodeUpdater.casNodeState(node, null, toState)) {
                LockSupport.unpark(node.getThread());
                count++;
                if (justOne) break;
            }
        }
        return count;
    }

    //****************************************************************************************************************//
    //                                          2: queue Methods(6)                                                   //
    //****************************************************************************************************************//
    protected final ThreadNode createNode() {
        return createNode(null);
    }

    protected final ThreadNode createNode(Object type) {
        return new ThreadNode(type);
    }

    protected final void removeNode(ThreadNode node) {
        waitQueue.remove(node);
    }

    protected final void appendNode(ThreadNode node) {
        waitQueue.offer(node);
    }

    protected final ThreadNode appendNewNode() {
        return appendNewNode(null);
    }

    protected final ThreadNode appendNewNode(Object type) {
        ThreadNode node = new ThreadNode(type);
        waitQueue.offer(node);
        return node;
    }

    //****************************************************************************************************************//
    //                                          3: Wakeup All                                                         //
    //****************************************************************************************************************//
    public final int wakeupAll() {
        return wakeupAll(SIGNAL);
    }

    public final int wakeupAll(Object toState) {
        return wakeup(waitQueue.iterator(), toState, null, false, null);
    }

    public final int wakeupAll(Object toState, Object nodeType) {
        return wakeup(waitQueue.iterator(), toState, null, false, nodeType);
    }

    //****************************************************************************************************************//
    //                                          4: Wakeup One                                                         //
    //****************************************************************************************************************//
    public final int wakeupOne() {
        return wakeupOne(SIGNAL);
    }

    public final int wakeupOne(boolean fromHead) {
        return wakeupOne(SIGNAL, fromHead);
    }

    public final int wakeupOne(ThreadNode skipNode) {
        return wakeupOne(SIGNAL, skipNode);
    }

    public final int wakeupOne(boolean fromHead, ThreadNode skipNode) {
        return wakeupOne(SIGNAL, fromHead, skipNode);
    }

    public final int wakeupOne(Object toState) {
        return wakeup(waitQueue.iterator(), toState, null, true, null);
    }

    public final int wakeupOne(Object toState, boolean fromHead) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), toState, null, true, null);
    }

    public final int wakeupOne(Object toState, ThreadNode skipNode) {
        return wakeup(waitQueue.iterator(), toState, skipNode, true, null);
    }

    public final int wakeupOne(Object toState, boolean fromHead, ThreadNode skipNode) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), toState, skipNode, true, null);
    }

    public final int wakeupOne(Object toState, Object nodeType) {
        return wakeup(waitQueue.iterator(), toState, null, true, nodeType);
    }

    public final int wakeupOne(Object state, Object nodeType, ThreadNode skipNode) {
        return wakeup(waitQueue.iterator(), state, skipNode, true, nodeType);
    }

    public final int wakeupOne(Object state, Object nodeType, boolean fromHead) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), state, null, true, nodeType);
    }

    public final int wakeupOne(Object state, Object nodeType, boolean fromHead, ThreadNode skipNode) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), state, skipNode, true, nodeType);
    }

    //****************************************************************************************************************//
    //                                         5: Monitor Methods(6)                                                  //
    //****************************************************************************************************************//
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

    public int getQueueLength(Object nodeType) {
        if (nodeType == null) return getQueueLength();

        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (equals(node.getType(), nodeType) && node.getState() == null) count++;
        }
        return count;
    }

    public Collection<Thread> getQueuedThreads(Object nodeType) {
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
                    wakeupOne(state, node);//send the got signal state to another waiter(skip over the current node during wakeup iterator)
                throw new InterruptedException();
            }
        }
    }
}
