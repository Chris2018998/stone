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

public class ResourceWaitPool {
    //result call pool
    private ResultWaitPool callPool;

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public ResourceWaitPool(boolean fair) {
        this(new ResultWaitPool(fair));
    }

    public ResourceWaitPool(ResultWaitPool callPool) {
        if (callPool == null) throw new IllegalArgumentException("call result pool can't be null");
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
    public final boolean tryAcquire(ResourceAction action, int size) {
        return action.tryAcquire(size);
    }

    //acquire
    public final boolean acquire(ResourceAction action, int size, ThreadParkSupport parker, boolean throwsIE, ThreadNode node, boolean wakeupOtherOnIE) throws InterruptedException {
        try {
            return tryAcquire(action, size) || callPool.doCallForNode(action, size, true, parker, throwsIE, node, wakeupOtherOnIE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

    //acquire
    public final boolean acquire(ResourceAction action, int size, ThreadParkSupport parker, boolean throwsIE, Object acquisitionType, boolean wakeupOtherOnIE) throws InterruptedException {
        try {
            return tryAcquire(action, size) || callPool.doCall(action, size, true, parker, throwsIE, acquisitionType, wakeupOtherOnIE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

    //release
    public final boolean release(ResourceAction action, int size) {
        if (action.tryRelease(size)) {
            callPool.wakeupOne();
            return true;
        } else {
            return false;
        }
    }
}
