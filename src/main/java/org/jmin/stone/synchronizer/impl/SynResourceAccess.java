/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.ResourceAccess;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.jmin.stone.synchronizer.impl.SynAcquireTypes.EXCLUSIVE_HOLD;
import static org.jmin.stone.synchronizer.impl.SynAcquireTypes.SHARABLE_HOLD;
import static org.jmin.stone.synchronizer.impl.ThreadNodeState.ACQUIRE;
import static org.jmin.stone.synchronizer.impl.ThreadNodeState.WAITING;

/**
 * A resource access synchronizer implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public class SynResourceAccess extends ThreadNodeChain implements ResourceAccess {
    //acquisition mode
    private boolean fairMode;
    //reentrant count atomic(total count)
    private AtomicInteger holdCount = new AtomicInteger(0);
    //accessing node reference(null or exclusive node,first sharable node)
    private AtomicReference<ThreadNode> currentHolder = new AtomicReference<>();
    //thread local hold count(reentrant)
    private ThreadLocal<HoldTraceInfo> holdTrace = new ThreadLocal<HoldTraceInfo>();
    //wait queue(contains exclusive and share)
    private ConcurrentLinkedQueue<ThreadNode> waitQueue = new ConcurrentLinkedQueue<>();

    //****************************************************************************************************************//
    //                                          1: Constructor(2)                                                     //
    //****************************************************************************************************************//
    public SynResourceAccess() {
    }

    public SynResourceAccess(boolean isFair) {
        this.fairMode = isFair;
    }

    //****************************************************************************************************************//
    //                                          2: monitor methods                                                    //
    //****************************************************************************************************************//
    //true,fair mode acquisition
    public boolean isFair() {
        return fairMode;
    }

    //true,if hold by current thread
    public boolean isHeldByCurrentThread() {
        HoldTraceInfo holdInfo = holdTrace.get();
        return holdInfo != null && holdInfo.holdCount > 0;
    }

    //return current thread hold count(reentrant)
    public int getHoldCountByCurrentThread() {
        HoldTraceInfo holdInfo = holdTrace.get();
        return holdInfo != null ? holdInfo.holdCount : 0;
    }

    //true,if hold with exclusive mode by current thread
    public boolean isExclusiveHeldByCurrentThread() {
        ThreadNode node = currentHolder.get();
        return node != null && node.getThread() == Thread.currentThread();
    }

    //true,if hold with shared mode by current thread
    public boolean isSharedHeldByCurrentThread() {
        HoldTraceInfo holdInfo = holdTrace.get();
        return holdInfo != null && holdInfo.holdCount > 0 && holdInfo.holdType == SHARABLE_HOLD;
    }

    //Threads waiting for shared lock
    public Collection<Thread> getQueuedSharedThreads() {
        return getQueuedThreadsByAcquireType(SHARABLE_HOLD);
    }

    //Threads waiting for exclusive lock
    public Collection<Thread> getQueuedExclusiveThreads() {
        return getQueuedThreadsByAcquireType(EXCLUSIVE_HOLD);
    }

    //Threads waiting for exclusive lock
    private Collection<Thread> getQueuedThreadsByAcquireType(int acquireType) {
        LinkedList<Thread> threadList = new LinkedList<Thread>();
        for (ThreadNode node : waitQueue) {
            int state = node.getState();
            if (acquireType == node.getValue() && state == WAITING || state == ACQUIRE)
                threadList.add(node.getThread());
        }
        return threadList;
    }

    //****************************************************************************************************************//
    //                                          3: acquire/release                                                    //
    //****************************************************************************************************************//
    //release a permit to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    public void release(boolean isExclusive) {

    }

    //try to acquire a permit,if interrupted,then ignore it and continue to try
    public boolean acquireUninterruptibly(boolean exclusive, long deadlineNs) {
        return false;
    }

    //try to acquire a permit,if interrupted,then throws InterruptedException
    public boolean acquire(boolean exclusive, long deadlineNs) throws InterruptedException {
        return false;
    }

    //hold info
    private static class HoldTraceInfo {
        private int holdType;
        private int holdCount;
    }
}
