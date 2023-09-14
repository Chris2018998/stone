/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.extend;

import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitTester;
import org.stone.shine.util.concurrent.synchronizer.base.ResultValidator;
import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;

import java.util.Collection;

/**
 * resource wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ResourceWaitPool {
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

    //****************************************************************************************************************//
    //                                          3: acquireWithType/release methods(4)                                 //
    //****************************************************************************************************************//
    public final boolean tryAcquire(ResourceAction action, int size) {
        return action.tryAcquire(size);
    }

    //1: acquire by sync config
    public final boolean acquire(ResourceAction action, int size, SyncVisitConfig config) throws InterruptedException {
        try {
            return (Boolean) callPool.get(action, size, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

    //2: acquire directly
    public final boolean acquire(ResourceAction action, int size, ResultValidator validator, SyncVisitTester tester,
                                 Object nodeType, Object nodeValue, long parkNanos, boolean allowInterruption,
                                 boolean propagatedOnSuccess) throws InterruptedException {
        try {
            return (Boolean) callPool.get(action, size, validator, tester, nodeType, nodeValue,
                    parkNanos, allowInterruption, propagatedOnSuccess);
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
            callPool.wakeupFirst();
            return true;
        }
        return false;
    }
}
