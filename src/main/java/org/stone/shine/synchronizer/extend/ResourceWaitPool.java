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

import org.stone.shine.synchronizer.ThreadWaitConfig;
import org.stone.shine.synchronizer.base.ResultWaitPool;

import java.util.Collection;

import static org.stone.shine.synchronizer.ThreadNodeState.SIGNAL;

/**
 * resource wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ResourceWaitPool {
    //result call pool
    private final ResultWaitPool callPool;

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public ResourceWaitPool() {
        this(false);
    }

    public ResourceWaitPool(boolean fair) {
        this.callPool = new ResultWaitPool(fair);
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

    public final int wakeupAll(Object nodeType) {
        return callPool.wakeupAll(SIGNAL, nodeType);
    }

    //****************************************************************************************************************//
    //                                          3: acquireWithType/release methods(4)                                 //
    //****************************************************************************************************************//
    public final boolean tryAcquire(ResourceAction action, int size) {
        return action.tryAcquire(size);
    }

    //acquireWithType
    public final boolean acquire(ResourceAction action, int size, ThreadWaitConfig config) throws InterruptedException {
        try {
            return (boolean) callPool.doCall(action, size, config);
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
