/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer;

import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;
import org.stone.shine.util.concurrent.synchronizer.validator.ResultEqualsValidator;

import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeStates.REMOVED;
import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.maxTimedSpins;

/**
 * State-ObjectWaitPool
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class StateWaitPool extends ObjectWaitPool {
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
     * @throws java.lang.InterruptedException caller waiting interrupted,then throws it
     */
    public final Object get(SyncVisitConfig config) throws InterruptedException {
        return get(config, validator);
    }

    /**
     * try to wait for expected state in pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param config    Visitor config
     * @param validator state validator
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws java.lang.InterruptedException caller waiting interrupted,then throws it
     */
    public final Object get(SyncVisitConfig config, ResultValidator validator) throws InterruptedException {
        //1:config check
        if (Thread.interrupted()) throw new InterruptedException();
        if (config == null || validator == null)
            throw new NullPointerException("Exists null argument,please check(syncConfig,validator)");

        //2:offer to wait queue
        SyncNode node = config.getSyncNode();
        int spins = appendAsWaitNode(node) ? maxTimedSpins : 0;//spin count

        //3:get control parameters from config
        ThreadParkSupport parkSupport = config.getParkSupport();

        //4: spin control（Logic from BeeCP）
        try {
            do {
                //4.1: read node state
                Object state = node.getState();
                //4.2: if state is not null,then test it
                if (state != null && validator.isExpected(state)) return state;

                //4.3: fail check
                if (parkSupport.isTimeout()) {
                    if (casState(node, state, REMOVED)) return validator.resultOnTimeout();
                } else if (parkSupport.isInterrupted() && config.isAllowInterruption()) {
                    if (casState(node, state, REMOVED)) throw new InterruptedException();
                } else if (state != null) {
                    node.setState(null);
                } else if (spins > 0) {
                    --spins;
                } else {
                    parkSupport.computeAndPark();
                }
            } while (true);
        } finally {
            waitQueue.remove(node);
        }
    }
}


