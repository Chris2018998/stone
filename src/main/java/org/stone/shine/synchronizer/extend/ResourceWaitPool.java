/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.extend;

import org.stone.shine.synchronizer.ThreadNode;
import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.base.ResultWaitPool;

import java.util.Collection;

/**
 * resource wait pool
 *
 * @author Chris Liaos
 * @version 1.0
 */

public abstract class ResourceWaitPool {

    //acquire action
    private ResourceAction action;

    //result call pool
    private ResultWaitPool callPool;

    //****************************************************************************************************************//
    //                                          1: constructors(3)                                                    //
    //****************************************************************************************************************//
    public ResourceWaitPool(ResourceAction action) {
        this(action, false);
    }

    public ResourceWaitPool(ResourceAction action, boolean fair) {
        this(action, new ResultWaitPool(fair));
    }

    public ResourceWaitPool(ResourceAction action, ResultWaitPool callPool) {
        if (action == null) throw new IllegalArgumentException("resource action can't be null");
        if (callPool == null) throw new IllegalArgumentException("call result pool can't be null");
        this.action = action;
        this.callPool = callPool;
    }

    //****************************************************************************************************************//
    //                                          2: monitor Methods(5)                                                 //
    //****************************************************************************************************************//
    public final boolean isFair() {
        return callPool.isFair();
    }

    public final int getQueueLength() {
        return callPool.getQueueLength();
    }

    public final boolean hasQueuedThreads() {
        return callPool.hasQueuedThreads();
    }

    public final Collection<Thread> getQueuedThreads() {
        return callPool.getQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return callPool.hasQueuedThread(thread);
    }

    //****************************************************************************************************************//
    //                                          3: acquire/release methods(4)                                         //
    //****************************************************************************************************************//
    protected final boolean tryAcquire(int size) {
        return action.tryAcquire(size);
    }

    //acquire
    protected final boolean acquire(int size, ThreadParkSupport support, boolean throwsIE, ThreadNode node, boolean wakeupOtherOnIE) throws InterruptedException {
        try {
            return tryAcquire(size) || callPool.doCallForNode(action, size, true, support, throwsIE, node, wakeupOtherOnIE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

    //acquire
    protected final boolean acquire(int size, ThreadParkSupport support, boolean throwsIE, Object acquisitionType, boolean wakeupOtherOnIE) throws InterruptedException {
        try {
            return tryAcquire(size) || callPool.doCall(action, size, true, support, throwsIE, acquisitionType, wakeupOtherOnIE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

    //release
    protected final boolean release(int size) {
        if (action.tryRelease(size)) {
            callPool.wakeupOne();
            return true;
        } else {
            return false;
        }
    }
}
