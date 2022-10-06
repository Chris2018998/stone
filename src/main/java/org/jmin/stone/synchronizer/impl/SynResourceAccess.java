/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.ResourceAccess;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.jmin.stone.synchronizer.impl.ThreadNodeState.WAITING;

/**
 * A resource access synchronizer implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public class SynResourceAccess extends ThreadNodeChain implements ResourceAccess {
    //true,acquire with fair mode
    private boolean fairMode;
    //Acquired count hold by thread,call method{#getHoldCountByCurrentThread} to get value,return 0,if not hold
    private ThreadLocal holdCountThreadLocal = new ThreadLocal();
    //Store acquired success node,which can be a exclusive node or first sharable acquired node
    private AtomicReference<ThreadNode> currentHoldNodeRef = new AtomicReference<>();
    //First sharable acquire node will fill it with compareAndSet(null,firstNode) and set to null when all sharable node released
    private AtomicReference<ThreadNode> firstShareNodeRef = new AtomicReference<>();
    //If set failed to <variable>firstShareNodeRef</variable>,then add to the shared chain and wait notification from the first share node
    private ThreadNodeChain sharableAccessWaitChain = new ThreadNodeChain();
    //Acquired success count of a exclusive thread or a set of shared threads.if hold in sharable,the value reach zero,then trigger shared real release.
    private AtomicInteger acquiredSuccessHoldCount = new AtomicInteger(0);

    public SynResourceAccess() {
    }

    public SynResourceAccess(boolean isFair) {
        this.fairMode = isFair;
    }

    //true,fair mode acquisition
    public boolean isFair() {
        return fairMode;
    }

    //true,if hold by current thread
    public boolean isHeldByCurrentThread() {
        return false;
    }

    //true,if hold with shared mode by current thread
    public boolean isSharedHeldByCurrentThread() {
        return false;
    }

    //true,if hold with exclusive mode by current thread
    public boolean isExclusiveHeldByCurrentThread() {
        return false;
    }

    //return current thread hold count(reentrant)
    public int getHoldCountByCurrentThread() {
        return 1;
    }

    //Threads waiting for shared lock
    public Thread[] getQueuedSharedThreads() {
        return null;
    }

    //Threads waiting for exclusive lock
    public Thread[] getQueuedExclusiveThreads() {
        return null;
    }

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

    //****************************************************************************************************************//
    //                                   Access node type define                                                      //
    //****************************************************************************************************************//
    private static class ShareHoldNode extends ThreadNode {
        private ShareHoldNode() {
            super(WAITING);
        }
    }

    //exclusive acquire node
    private static class ExclusiveHoldNode extends ThreadNode {
        private ExclusiveHoldNode() {
            super(WAITING);
        }
    }

//   //Just support exclusive node,condition contains a wait chain,its node can be move to access acquire chain,when call signal and signalAll
//    private static class ConditionImpl extends ThreadNodeChain implements Condition {
//
//    }
}
