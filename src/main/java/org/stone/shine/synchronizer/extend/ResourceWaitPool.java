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

    //result call pool
    protected ResultWaitPool callPool;

    //constructor with a result call pool
    public ResourceWaitPool(ResultWaitPool callPool) {
        this.callPool = callPool;
    }

    //****************************************************************************************************************//
    //                                          1: monitor Methods(5)                                                 //
    //****************************************************************************************************************//
    public final boolean isFair() {
        return callPool.isFair();
    }

    public final boolean hasQueuedThreads() {
        return callPool.hasQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return callPool.hasQueuedThread(thread);
    }

    public final int getQueueLength() {
        return callPool.getQueueLength();
    }

    public final Collection<Thread> getQueuedThreads() {
        return callPool.getQueuedThreads();
    }

    //****************************************************************************************************************//
    //                                          2: acquire methods(3)                                                 //
    //****************************************************************************************************************//
    protected boolean tryAcquire(ResourceAction action, int size) {
        return action.tryAcquire(size);
    }

    //acquire type method
    protected boolean acquire(ResourceAction action, int size, ThreadParkSupport support, boolean throwsIE, Object acquisitionType, boolean wakeupOtherOnIE) throws InterruptedException {
        try {
            return callPool.doCall(action, size, true, support, throwsIE, acquisitionType, wakeupOtherOnIE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

    //acquire type method
    protected boolean acquire(ResourceAction action, int size, ThreadParkSupport support, boolean throwsIE, ThreadNode node, boolean wakeupOtherOnIE) throws InterruptedException {
        try {
            return callPool.doCallForNode(action, size, true, support, throwsIE, node, wakeupOtherOnIE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }
}
