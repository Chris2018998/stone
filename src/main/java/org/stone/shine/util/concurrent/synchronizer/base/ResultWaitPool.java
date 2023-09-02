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
import org.stone.shine.util.concurrent.synchronizer.SyncNodeParker;
import org.stone.shine.util.concurrent.synchronizer.SyncNodeWaitPool;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.base.validator.ResultEqualsValidator;

import static org.stone.tools.CommonUtil.emptyMethod;

/**
 * Result Wait Pool,Outside caller to call pool's get method to take an object(execute ResultCall),
 * if not expected,then wait in pool util being wake-up by other or timeout/interrupted,and leave from pool.
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
        if (config.getVisitTester().test(fair, waitQueue.peek(), config)) {
            Object result = call.call(arg);
            if (validator.isExpected(result))
                return result;
        }

        //3: offer to wait queue
        boolean atFirst;
        byte spins = 0, postSpins = 1;
        SyncNode node = config.getSyncNode();
        if (atFirst = appendAsWaitNode(node)) spins = postSpins = 3;//self-in
        SyncNodeParker parkSupport = config.getParkSupport();

        //4: spin
        do {
            //4.1: execute call when node at first of queue
            if (atFirst) {
                try {
                    Object result = call.call(arg);
                    if (validator.isExpected(result)) {
                        waitQueue.poll();
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
                emptyMethod();
            } else {
                do {
                    node.setStateWhenNotNull(null);
                    boolean failed = parkSupport.tryPark();//timeout or interrupted
                    if (!atFirst) atFirst = waitQueue.peek() == node;
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
