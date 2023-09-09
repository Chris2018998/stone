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

import org.stone.shine.util.concurrent.synchronizer.*;
import org.stone.shine.util.concurrent.synchronizer.base.validator.ResultEqualsValidator;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;

/**
 * Result Wait Pool,Outside caller to call pool's get method to take an object(execute ResultCall),
 * if not expected,then wait in pool util being wake-up by other or timeout/interrupted, and leave from pool.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ResultWaitPool extends SyncNodeWaitPool {
    //true,use fair mode
    private final boolean fair;
    //result validator(bool validator is default)
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
        super(new SyncNodeChain());
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
     * @throws java.lang.Exception from call or InterruptedException after thread park
     */
    public final Object get(ResultCall call, Object arg, SyncVisitConfig config) throws Exception {
        return this.get(call, arg, validator, config);
    }

    /**
     * @param call      executed in pool to get result
     * @param arg       call argument
     * @param config    thread wait config
     * @param validator result validator
     * @return passed result
     * @throws java.lang.Exception from call or InterruptedException after thread park
     */
    public final Object get(ResultCall call, Object arg, ResultValidator validator, SyncVisitConfig config) throws Exception {
        //1:check call parameter
        if (Thread.interrupted()) throw new InterruptedException();
        if (call == null || config == null || validator == null)
            throw new IllegalArgumentException("Illegal argument,please check(call,validator,syncConfig)");

        //2:test before call,if passed,then execute call
        if (config.getVisitTester().test(fair, waitQueue.peek(), config.getNodeType())) {
            Object result = call.call(arg);
            if (validator.isExpected(result))
                return result;
        }

        //3:offer to wait queue
        byte spins = 1, postSpins = 1;
        ThreadParkSupport parkSupport = null;
        SyncNode node = config.getSyncNode();
        boolean atFirst = appendAsWaitNode(node);

        //4: spin
        do {
            if (atFirst) {//4.1: execute result call
                try {
                    do {
                        Object result = call.call(arg);
                        if (validator.isExpected(result)) {
                            waitQueue.poll();
                            if (config.isPropagatedOnSuccess()) wakeupFirst(node.getType());
                            return result;
                        }
                    } while (spins > 0 && --spins > 0);
                    spins = postSpins = (byte) (postSpins << 1);
                } catch (Throwable e) {
                    waitQueue.poll();
                    wakeupFirst();
                    throw e;
                }
            }

            //4.2: prepare parkSupport
            if (parkSupport == null) parkSupport = config.getParkSupport();

            //4.3: try to park
            if (node.getState() == RUNNING) {
                node.setState(null);
            } else if (parkSupport.computeAndPark()) {//timeout or interrupted
                if (parkSupport.isTimeout()) {
                    removeAndWakeupFirst(node);
                    return validator.resultOnTimeout();
                } else if (config.isAllowInterruption()) {
                    removeAndWakeupFirst(node);
                    throw new InterruptedException();
                }
            }
            //4.4: check node pos
            if (!atFirst) atFirst = waitQueue.peek() == node;
        } while (true);
    }
}
