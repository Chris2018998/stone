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

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;
import static org.stone.tools.CommonUtil.maxTimedSpins;

/**
 * Result Wait Pool, Outside caller to call pool's get method to take an object(execute ResultCall),
 * if not expected,then wait in pool util being wake-up by other or timeout or interrupted, and leave from pool.
 * Some Key points about pool are below
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
     * @throws java.lang.Exception from call or InterruptedException after thread tryToPark
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
     * @throws java.lang.Exception from call or InterruptedException after thread tryToPark
     */
    public final Object get(ResultCall call, Object arg, ResultValidator validator, SyncVisitConfig config) throws Exception {
        //1:check call parameter
        if (Thread.interrupted()) throw new InterruptedException();
        if (call == null || config == null || validator == null)
            throw new IllegalArgumentException("Illegal argument,please check(call,validator,syncConfig)");

        //2:test before call
        if (config.getVisitTester().test(fair, firstNode(), config)) {
            Object result = call.call(arg);
            if (validator.isExpected(result))
                return result;
        }

        //3:offer to wait queue
        int spins = 0;//spin count
        boolean isAtFirst;
        SyncNode node = config.getSyncNode();
        if (isAtFirst = appendAsWaitNode(node))  //self-in
            spins = maxTimedSpins;

        //4:get control parameters from config
        boolean success = false;
        ThreadParkSupport parkSupport = config.getParkSupport();
        //5:spin control（Logic from BeeCP）
        try {
            do {
                //5.1: execute call(got a signal or at first of wait queue)
                if (isAtFirst || (isAtFirst = atFirst(node))) {
                    Object result = call.call(arg);
                    if (validator.isExpected(result)) {
                        success = true;
                        return result;
                    }
                }

                //5.2: fail check
                if (parkSupport.isTimeout())
                    return validator.resultOnTimeout();
                if (parkSupport.isInterrupted() && config.isAllowInterruption())
                    throw new InterruptedException();

                if (spins > 0) //5.4: decr spin count
                    --spins;
                else { //5.5: try to park
                    if (node.getState() != null) node.setState(null);
                    parkSupport.tryToPark();
                }
            } while (true);
        } finally {
            this.removeNode(isAtFirst, node);//self-out
            if (success) {
                if (config.isPropagatedOnSuccess())
                    this.wakeupFirst(true, node.getType(), RUNNING);//same type
            } else {
                this.wakeupFirst(true, null, RUNNING);//any type
            }
        }
    }
}
