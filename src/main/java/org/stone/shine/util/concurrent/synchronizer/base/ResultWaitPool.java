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

/**
 * Result Wait Pool,Outside caller to call pool's get method to take an object(execute ResultCall),
 * if not expected,then wait in pool util being wake-up by other or timeout/interrupted, and leave from pool.
 * <p>
 * Some Key points about pool are below
 * 1) Assume that exists a runnable permit,who get it who run
 * 2) The permit can be transferred among with these waiters
 * 3) If a waiter is at first of the wait queue,it will get the permit automatically
 * 4) When waiters leave from pool,they should check whether getting the permit(transferred),maybe transfer it to other
 * 4.1) If failure in pool(timeout or interrupted), need transfer it to next waiter.
 * 4.2) if success and indicator of propagation is true,need transfer to next
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ResultWaitPool extends ThreadWaitingPool {
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
     * @throws java.lang.Exception from call or InterruptedException after thread tryPark
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
     * @throws java.lang.Exception from call or InterruptedException after thread tryPark
     */
    public final Object get(ResultCall call, Object arg, ResultValidator validator, SyncVisitConfig config) throws Exception {
        //1: check call parameter
        if (Thread.interrupted()) throw new InterruptedException();
        if (call == null || config == null || validator == null)
            throw new IllegalArgumentException("Illegal argument,please check(call,validator,syncConfig)");

        //2: test before call,if passed,then execute call
        if (config.getVisitTester().test(fair, peekFirst(), config)) {
            Object result = call.call(arg);
            if (validator.isExpected(result))
                return result;
        }

        //3: offer to wait queue
        boolean atFirst;
        byte spins = 0, postSpins = 1;
        SyncNode node = config.getSyncNode();
        if (atFirst = appendAsWaitNode(node)) spins = postSpins;//self-in
        ThreadParkSupport parkSupport = config.getParkSupport();

        //4: spin
        do {
            //4.1: execute call when node at first of queue
            if (atFirst) {
                try {
                    Object result = call.call(arg);
                    if (validator.isExpected(result)) {
                        pollFirst();
                        if (config.isPropagatedOnSuccess()) wakeupFirst(node.getType());
                        return result;
                    }
                } catch (Throwable e) {
                    wakeupFirstOnFailure(node, true);
                    throw e;
                }
            }

            //4.2: decr spin count
            if (spins > 0) {
                --spins;
            } else {
                do {
                    node.setStateWhenNotNull(null);
                    boolean failed = parkSupport.tryPark();//timeout or interrupted
                    if (!atFirst) atFirst = atFirst(node);
                    if (failed) {
                        if (parkSupport.isTimeout()) {
                            wakeupFirstOnFailure(node, atFirst);
                            return validator.resultOnTimeout();
                        } else if (config.isAllowInterruption()) {
                            wakeupFirstOnFailure(node, atFirst);
                            throw new InterruptedException();
                        }
                    }
                } while (!atFirst);

                spins = postSpins = (byte) (postSpins << 1);//idea from JDK
            }
        } while (true);
    }
}
