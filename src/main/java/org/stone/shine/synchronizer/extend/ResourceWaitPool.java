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

    protected boolean tryAcquire(ResourceAction action, int size) {
        return action.tryAcquire(size);
    }

    protected boolean acquire(ResourceAction action, int size, ThreadParkSupport support, boolean throwsIE, Object acquisitionType) throws InterruptedException {
        try {
            return callPool.doCall(action, size, true, support, throwsIE, acquisitionType);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

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
