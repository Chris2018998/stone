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
import static org.stone.tools.CommonUtil.maxTimedSpins;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Core Desc: Do some thing(execute result call),if success,return directly, but if failed,
 * then wait util other's wakeup to continue doing the same thing.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ResultWaitPool extends ThreadWaitingPool {
    //true,use fair mode
    private final boolean fair;
    //result validator(equals validator is default)
    private final ResultValidator validator;

    //****************************************************************************************************************//
    //                                          1: constructors(3)                                                    //
    //****************************************************************************************************************//
    public ResultWaitPool() {
        this(false, ResultEqualsValidator.BOOL_EQU_VALIDATOR);
    }

    public ResultWaitPool(boolean fair) {
        this(fair, ResultEqualsValidator.BOOL_EQU_VALIDATOR);
    }

    public ResultWaitPool(boolean fair, ResultValidator validator) {
        this.fair = fair;
        this.validator = validator;
    }

    public final boolean isFair() {
        return this.fair;
    }

    /**
     * execute the call inside pool and match its result with a validator,if passed the return result value;
     * false then wait util other's wakeup to execute call again.
     *
     * @param call   executed in pool to get result
     * @param arg    call argument
     * @param config thread wait config
     * @return object, if call result check passed by validator
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final Object doCall(ResultCall call, Object arg, SyncVisitConfig config) throws Exception {
        return this.doCall(call, arg, validator, config);
    }

    /**
     * @param call      executed in pool to get result
     * @param arg       call argument
     * @param config    thread wait config
     * @param validator result validator
     * @return passed result
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final Object doCall(ResultCall call, Object arg, ResultValidator validator, SyncVisitConfig config) throws Exception {
        //1:check call parameter
        if (call == null) throw new IllegalArgumentException("Result call can't be null");
        if (config == null) throw new IllegalArgumentException("Visit config can't be null");
        if (validator == null) throw new IllegalArgumentException("Result validator can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        //2:execute call
        if (!fair || !this.hasQueuedPredecessors()) {
            Object result = call.call(arg);
            if (validator.isExpected(result)) return result;
        }

        //3:offer to wait queue
        config.setNodeInitState(RUNNING);
        SyncNode node = config.getSyncNode();
        int spins = appendAsWaitNode(node) ? maxTimedSpins : 0;//spin count

        //4:get control parameters from config
        boolean success = false;
        boolean allowInterrupted = config.supportInterrupted();
        ThreadParkSupport parkSupport = config.getParkSupport();

        //5:spin control（Logic from BeeCP）
        try {
            do {
                //5.1: read node state
                Object state = node.getState();
                if (state != null) {
                    if (state == RUNNING) {//RUNNING is a signal of wakeup
                        Object result = call.call(arg);
                        if (validator.isExpected(result)) {
                            success = true;
                            return result;
                        }
                    } else if (state == TIMEOUT)
                        return validator.resultOnTimeout();
                    else if (state == INTERRUPTED)
                        throw new InterruptedException();
                }

                //5.2: fail check
                if (parkSupport.isTimeout()) {
                    casState(node, state, TIMEOUT);
                } else if (parkSupport.isInterrupted() && allowInterrupted) {
                    casState(node, state, INTERRUPTED);
                } else if (state != null) {
                    node.setState(null);
                    Thread.yield();
                } else if (spins > 0) {
                    --spins;
                } else if (parkSupport.computeParkNanos() > spinForTimeoutThreshold) {
                    parkSupport.park();
                }
            } while (true);
        } finally {
            if (success)
                this.leaveFromWaitQueue(node, config.isWakeupNextOnSuccess(), true, config.getWakeupNodeTypeOnSuccess(), RUNNING);
            else
                this.leaveFromWaitQueue(node, config.isWakeupNextOnFailure(), true, config.getWakeupNodeTypeOnFailure(), RUNNING);
        }
    }
}
