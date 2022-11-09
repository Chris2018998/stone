/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.locks;

import org.stone.shine.synchronizer.ThreadNode;
import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.base.ResultWaitPool;

import java.util.concurrent.locks.Lock;

/**
 * work as lock core driver
 * <p>
 * Design a common driver with plugin for most lock types
 *
 * @author Chris Liao
 * @version 1.0
 */

abstract class LockCoreDriver implements Lock {

    //result call pool
    private ResultWaitPool callPool;

    public LockCoreDriver(ResultWaitPool callPool) {
        this.callPool = callPool;
    }

    //****************************************************************************************************************//
    //                                          2: lock methods                                                       //
    //****************************************************************************************************************//

    protected void acquireForConditionNode(LockAction action, ThreadParkSupport support, ThreadNode conditionNode) {
        try {
            callPool.doCallForNode(action, 1, true, support, false, conditionNode, false);
        } catch (Exception e) {
            //do nothing
        }
    }

    protected boolean acquireByAction(LockAction action, ThreadParkSupport support, boolean throwsIE, Object acquisitionType) throws InterruptedException {
        try {
            return callPool.doCall(action, 1, true, support, throwsIE, acquisitionType);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }
}
