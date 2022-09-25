/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.ResourceAccess;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.jmin.stone.synchronizer.impl.ThreadNodeState.WAIT_EXCLUSIVE;
import static org.jmin.stone.synchronizer.impl.ThreadNodeState.WAIT_SHARED;

/**
 * A resource access synchronizer implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public class SynResourceAccess extends ThreadNodeChain implements ResourceAccess {
    //true,acquire with fair mode
    private boolean fairMode;
    //reentrant hold count of thread
    private ThreadLocal holdCountThreadLocal = new ThreadLocal();
    //first node in sharable acquiring or waiting for sharable access acquisition
    private AtomicReference<ThreadNode> firstShareNodeRef = new AtomicReference<>();
    //acquired node(ExclusiveHoldNode,firstShareHoldNode)
    private AtomicReference<ThreadNode> currentHoldNodeRef = new AtomicReference<>();

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
    //current node will append to chain,others share request node will offer to its inner queue
    private static class ShareHoldNode extends ThreadNode {
        //add other acquire share request nodes
        private ConcurrentLinkedQueue<ShareHoldNode> waitQueue;

        private ShareHoldNode() {
            super(WAIT_SHARED);
            this.waitQueue = new ConcurrentLinkedQueue<>();
        }

        public void addNode(ShareHoldNode node) {
            waitQueue.offer(node);
        }

        public void removeNode(ShareHoldNode node) {
            waitQueue.remove(node);
        }

        //wakeup other wait node in queue,when current node has gotten a shared access
        public void wakeup() {
            //@todo
        }
    }

    //exclusive acquire node
    private static class ExclusiveHoldNode extends ThreadNode {
        private ExclusiveHoldNode() {
            super(WAIT_EXCLUSIVE);
        }
    }

//   //Just support exclusive node,condition contains a wait chain,its node can be move to access acquire chain,when call signal and signalAll
//    private static class ConditionImpl extends ThreadNodeChain implements Condition {
//
//    }
}
