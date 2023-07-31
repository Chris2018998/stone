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
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.ThreadParkSupport;
import org.stone.shine.util.concurrent.synchronizer.ThreadWaitingPool;
import org.stone.shine.util.concurrent.synchronizer.base.validator.ResultEqualsValidator;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.*;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Wait to get a expected state from pool
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
     * @param config Visitor config
     * @return a expected state
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final Object doWait(SyncVisitConfig config) throws InterruptedException {
        return doWait(config, validator);
    }

    /**
     * try to wait for expected state in pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param config    Visitor config
     * @param validator state validator
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final Object doWait(SyncVisitConfig config, ResultValidator validator) throws InterruptedException {
        //1:config check
        if (config == null) throw new IllegalArgumentException("Visitor Config can't be null");
        if (validator == null) throw new IllegalArgumentException("State validator can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        //2:offer to wait queue
        SyncNode node = appendNode(config.getSyncNode());

        //3:get control parameters from config
        boolean allowInterrupted = config.supportInterrupted();
        ThreadParkSupport parkSupport = config.getParkSupport();

        //4: spin control（Logic from BeeCP）
        try {
            do {
                //4.1: read node state
                Object state = node.getState();
                //4.2: if state is not null,then test it
                if (state != null) {
                    if (state == TIMEOUT) return validator.resultOnTimeout();
                    if (state == INTERRUPTED) throw new InterruptedException();
                    if (validator.isExpected(state)) return state;
                }

                //4.3: fail check
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
            //here:don't wakeup other
            this.leaveFromWaitQueue(node, false, true, node.getType(), RUNNING);
        }
    }
}


