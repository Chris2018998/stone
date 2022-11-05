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
 * base wait pool
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
    private static int wakeup(Iterator<ThreadNode> iterator, Object state, Object equalsValue, boolean justOne, ThreadNode skipNode) {
        int count = 0;
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (node == skipNode) continue;
            if (equals(node.getValue(), equalsValue) && ThreadNodeUpdater.casNodeState(node, null, state)) {
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

    protected final ThreadNode createNode(Object value) {
        return new ThreadNode(value);
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

    protected final ThreadNode appendNewNode(Object value) {
        ThreadNode node = new ThreadNode(value);
        waitQueue.offer(node);
        return node;
    }

    //****************************************************************************************************************//
    //                                          3: Wakeup to default state: SIGNAL(5)                                 //
    //****************************************************************************************************************//
    public final int wakeupAll() {
        return wakeupAllToState(SIGNAL);
    }

    public final int wakeupOne() {
        return wakeupOneToState(SIGNAL);
    }

    public final int wakeupOne(ThreadNode skipNode) {
        return wakeupOneToState(SIGNAL, skipNode);
    }

    public final int wakeupOne(boolean fromHead) {
        return wakeupOneToState(SIGNAL, fromHead);
    }

    public final int wakeupOne(boolean fromHead, ThreadNode skipNode) {
        return wakeupOneToState(SIGNAL, fromHead, skipNode);
    }

    //****************************************************************************************************************//
    //                                          4: Wakeup to specified state(5)                                       //
    //****************************************************************************************************************//
    public final int wakeupAllToState(Object state) {
        return wakeup(waitQueue.iterator(), state, null, false, null);
    }

    public final int wakeupOneToState(Object state) {
        return wakeup(waitQueue.iterator(), state, null, true, null);
    }

    public final int wakeupOneToState(Object state, ThreadNode skipNode) {
        return wakeup(waitQueue.iterator(), state, null, true, skipNode);
    }

    public final int wakeupOneToState(Object state, boolean fromHead) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), state, null, true, null);
    }

    public final int wakeupOneToState(Object state, boolean fromHead, ThreadNode skipNode) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), state, null, true, skipNode);
    }

    //****************************************************************************************************************//
    //                                          5: Wakeup to state by node value(5)                                   //
    //****************************************************************************************************************//
    public final int wakeupAllToState(Object state, Object equalsValue) {
        return wakeup(waitQueue.iterator(), equalsValue, state, false, null);
    }

    public final int wakeupOneToState(Object state, Object equalsValue) {
        return wakeup(waitQueue.iterator(), state, equalsValue, true, null);
    }

    public final int wakeupOneToState(Object state, Object equalsValue, ThreadNode skipNode) {
        return wakeup(waitQueue.iterator(), state, equalsValue, true, skipNode);
    }

    public final int wakeupOneToState(Object state, Object equalsValue, boolean fromHead) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), state, equalsValue, true, null);
    }

    public final int wakeupOneToState(Object state, Object equalsValue, boolean fromHead, ThreadNode skipNode) {
        return wakeup(fromHead ? waitQueue.iterator() : waitQueue.descendingIterator(), state, equalsValue, true, skipNode);
    }

    //****************************************************************************************************************//
    //                                         6: Monitor Methods(6)                                                  //
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

    public int getQueueLength(Object value) {
        int count = 0;
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (value.equals(node.getValue()) && node.getState() == null) count++;
        }
        return count;
    }

    public Collection<Thread> getQueuedThreads(Object value) {
        LinkedList<Thread> threadList = new LinkedList<>();
        Iterator<ThreadNode> iterator = waitQueue.iterator();
        while (iterator.hasNext()) {
            ThreadNode node = iterator.next();
            if (value.equals(node.getValue()) && node.getState() == null) threadList.add(node.getThread());
        }
        return threadList;
    }

    //****************************************************************************************************************//
    //                                         7: Park methods(2)                                                     //
    //****************************************************************************************************************//
    protected final void parkNodeThread(ThreadNode node, ThreadParkSupport support, boolean throwsIE) throws InterruptedException {
        parkNodeThread(node, support, throwsIE, true);
    }

    protected final void parkNodeThread(ThreadNode node, ThreadParkSupport support, boolean throwsIE, boolean wakeupOtherOnIE) throws InterruptedException {
        if (support.calculateParkTime()) {//before deadline
            if (support.park() && throwsIE) {//interrupted
                if (node.getState() == null) {
                    //try to cas state to INTERRUPTED,if failed,will reach step2
                    if (ThreadNodeUpdater.casNodeState(node, null, INTERRUPTED))
                        throw new InterruptedException();
                }

                //step2:send signal state to other when got
                Object state = node.getState();
                if (state != null && wakeupOtherOnIE)
                    wakeupOneToState(state, node);//send the signal state to another waiter(skip over the current node during wakeup iterator)
                throw new InterruptedException();
            }
        }
    }
}
