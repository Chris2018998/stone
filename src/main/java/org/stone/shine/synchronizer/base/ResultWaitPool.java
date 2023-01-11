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

import org.stone.shine.synchronizer.CasNode;
import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.ThreadWaitConfig;
import org.stone.shine.synchronizer.ThreadWaitPool;
import org.stone.shine.synchronizer.base.validator.ResultEqualsValidator;

import static org.stone.shine.synchronizer.CasNodeUpdater.casState;
import static org.stone.shine.synchronizer.CasStaticState.SIGNAL;

/**
 * execute the call inside pool and match its result with a validator,if passed the return result value;
 * false then wait util other's wakeup to execute call again.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ResultWaitPool extends ThreadWaitPool {
    //true,use fair mode
    private final boolean fair;

    //result validator(equals validator is default)
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
     * execute the call inside pool and match its result with a validator,if passed the return result value;
     * false then wait util other's wakeup to execute call again.
     *
     * @param call   executed in pool to get result
     * @param arg    call argument
     * @param config thread wait config
     * @return object, if call result check passed by validator
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final Object doCall(ResultCall call, Object arg, ThreadWaitConfig config) throws Exception {
        return this.doCall(call, arg, validator, config);
    }

    /**
     * execute the call inside pool and match its result with a validator,if passed the return result value;
     * false then wait util other's wakeup to execute call again.
     *
     * @param call      executed in pool to get result
     * @param arg       call argument
     * @param validator result call validator
     * @param config    thread wait config
     * @return object, if call result check passed by validator
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final Object doCall(ResultCall call, Object arg, ResultValidator validator, ThreadWaitConfig config) throws Exception {
        //1:check call parameter
        if (call == null) throw new IllegalArgumentException("call can't be null");
        if (config == null) throw new IllegalArgumentException("wait config can't be null");
        if (validator == null) throw new IllegalArgumentException("result validator can't be null");

        //2:try to execute call
        if (config.isOutsideOfWaitPool()) {
            if (!fair || !this.hasQueuedThreads()) {
                Object result = call.call(arg);
                if (validator.isExpected(result)) return result;
            }
            super.appendNode(config.getCasNode());
        }

        //3:get wait node from config object
        final CasNode node = config.getCasNode();

        //4:get control parameters from config
        final boolean throwsIE = config.isAllowThrowsIE();
        final boolean wakeupOtherOnIE = config.isTransferSignalOnIE();
        final ThreadParkSupport parker = config.getThreadParkSupport();

        //5:spin control
        try {
            do {
                //5.1: read node state
                Object state = node.getState();
                if (state == null || casState(node, null, SIGNAL)) state = SIGNAL;

                //5.2: execute call
                Object result = call.call(arg);
                if (validator.isExpected(result)) return result;

                //5.3: timeout test
                if (parker.isTimeout()) {
                    return validator.resultOnTimeout();
                } else {
                    node.setState(null);
                    Thread.yield();
                    parkNodeThread(node, parker, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
