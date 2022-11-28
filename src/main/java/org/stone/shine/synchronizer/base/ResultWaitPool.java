/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.base;

import org.stone.shine.synchronizer.*;
import org.stone.shine.synchronizer.base.validator.ResultEqualsValidator;

/**
 * Result call executed inside pool and compare result of call with expected parameter,if equals,return true
 * and leave from pool,not equals,then join in wait queue util wakeup from other and execute call again and compare.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ResultWaitPool extends ThreadWaitPool {

    //true,use fair mode to execute call
    private final boolean fair;

    //call result validator(equals validator is default)
    private final ResultValidator validator;

    //****************************************************************************************************************//
    //                                          1: constructors(3)                                                    //
    //****************************************************************************************************************//
    public ResultWaitPool() {
        this(false);
    }

    public ResultWaitPool(boolean fair) {
        this(fair, new ResultEqualsValidator(true, false));
    }

    public ResultWaitPool(boolean fair, ResultValidator validator) {
        this.fair = fair;
        this.validator = validator;
    }

    public final boolean isFair() {
        return this.fair;
    }

    /**
     * execute the call inside pool and compare its result with expected parameter value,if equivalence that this
     * method return true,not equals that wait timeout and return false.
     *
     * @param call   executed in pool to get result
     * @param arg    call argument
     * @param config thread wait config
     * @return object, if call result check passed by validator
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final Object doCall(ResultCall call, Object arg, ThreadWaitConfig config) throws Exception {
        return this.doCall(call, arg, config, validator);
    }

    /**
     * execute the call inside pool and compare its result with expected parameter value,if equivalence that this
     * method return true,not equals that wait timeout and return false.
     *
     * @param call      executed in pool to get result
     * @param arg       call argument
     * @param config    thread wait config
     * @param validator result call validator
     * @return object, if call result check passed by validator
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final Object doCall(ResultCall call, Object arg, ThreadWaitConfig config, ResultValidator validator) throws Exception {
        //1:check call parameter
        if (call == null) throw new IllegalArgumentException("call can't be null");
        if (config == null) throw new IllegalArgumentException("wait config can't be null");

        //2:try to execute call
        if (config.isOutsideOfWaitPool()) {
            if (!fair || !this.hasQueuedThreads()) {
                Object result = call.call(arg);
                if (validator.validate(result)) return result;
            }
            super.appendNode(config.getThreadNode());
        }

        //3:get wait node from config object
        ThreadNode node = config.getThreadNode();

        //4:get control parameters from config
        boolean throwsIE = config.isThrowsIE();
        boolean wakeupOtherOnIE = config.isTransferSignalOnIE();

        //5:create thread parker
        ThreadParkSupport parker = config.getThreadParkSupport();

        //6:spin control
        try {
            do {
                //6.1:if got a signal then execute call
                Object result = call.call(arg);
                if (validator.validate(result)) return result;

                //6.2:read node state
                Object state = node.getState();

                //6.3:timeout test
                if (parker.isTimeout()) {
                    //6.3.1:try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return false(abandon)
                    if (ThreadNodeUpdater.casNodeState(node, state, ThreadNodeState.TIMEOUT))
                        return validator.resultOnTimeout();
                } else if (state != null) {//6.4:reach here means not got expected value from call,then rest to continue waiting
                    node.setState(null);
                    Thread.yield();
                } else {//here: state == null
                    //6.5:park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, parker, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
