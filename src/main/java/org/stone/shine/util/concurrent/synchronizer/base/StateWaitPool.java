/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.base;

import org.stone.shine.util.concurrent.synchronizer.SyncNode;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitorConfig;
import org.stone.shine.util.concurrent.synchronizer.ThreadParkSupport;
import org.stone.shine.util.concurrent.synchronizer.ThreadWaitingPool;
import org.stone.shine.util.concurrent.synchronizer.base.validator.ResultEqualsValidator;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.*;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Expected state wait pool,which can be applied in ThreadPoolExecutor implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public class StateWaitPool extends ThreadWaitingPool {

    //state validator
    private final ResultValidator validator;

    public StateWaitPool(ResultValidator validator) {
        this.validator = validator;
    }

    public StateWaitPool(Object expectState) {
        this.validator = new ResultEqualsValidator(expectState, false);
    }

    /**
     * try to wait for expected state in pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param config thread wait config
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final Object doWait(SyncVisitorConfig config) throws InterruptedException {
        return doWait(validator, config);
    }

    /**
     * try to wait for expected state in pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param validator result call validator
     * @param config    thread wait config
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final Object doWait(ResultValidator validator, SyncVisitorConfig config) throws InterruptedException {
        //1:check
        if (config == null) throw new IllegalArgumentException("wait config can't be null");
        if (validator == null) throw new IllegalArgumentException("result validator can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        //2:append to wait queue
        SyncNode node = config.getSyncNode();
        super.appendNode(node);

        //3:get control parameters from config
        boolean successGot = false;
        boolean allowInterrupted = config.isSupportInterrupted();
        ThreadParkSupport parkSupport = config.getParkSupport();

        //3: spin control
        try {
            do {
                //3.1: read node state
                Object state = node.getState();
                //3.2: if state is not null,then test it
                if (state != null) {
                    if (validator.isExpected(state)) {
                        successGot = true;
                        return state;
                    }

                    if (state == TIMEOUT) return validator.resultOnTimeout();
                    if (state == INTERRUPTED) throw new InterruptedException();
                }

                //3.3: fail check
                if (parkSupport.isTimeout()) {
                    casState(node, state, TIMEOUT);
                } else if (parkSupport.isInterrupted() && allowInterrupted) {
                    casState(node, state, INTERRUPTED);
                } else if (state != null) {
                    node.setState(null);
                    Thread.yield();
                } else if (parkSupport.computeParkNanos() > spinForTimeoutThreshold) {
                    parkSupport.park();
                }
            } while (true);
        } finally {
            boolean wakeupOne = successGot ? config.isWakeupNextOnSuccess() : config.isWakeupNextOnFailure();
            Object nodeType = (wakeupOne && config.isWakeupSameType()) ? node.getType() : null;
            super.leaveFromPool(node, wakeupOne, true, nodeType, RUNNING);
        }
    }
}


