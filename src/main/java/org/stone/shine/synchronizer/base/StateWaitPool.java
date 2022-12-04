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
 * Expected state wait pool,which can be applied in ThreadPoolExecutor implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public class StateWaitPool extends ThreadWaitPool {

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
    public final Object doWait(ThreadWaitConfig config) throws InterruptedException {
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
    public final Object doWait(ResultValidator validator, ThreadWaitConfig config) throws InterruptedException {
        if (config == null) throw new IllegalArgumentException("wait config can't be null");
        if (validator == null) throw new IllegalArgumentException("result validator can't be null");

        //1:create wait node and offer to wait queue
        CasNode node = config.getThreadNode();
        if (config.isOutsideOfWaitPool()) super.appendNode(node);

        //2:get control parameters from config
        boolean throwsIE = config.isThrowsIE();
        boolean wakeupOtherOnIE = config.isTransferSignalOnIE();

        //3:create thread parker
        ThreadParkSupport parker = config.getThreadParkSupport();

        //4:spin control
        try {
            do {
                //4.1: read node state
                Object state = node.getState();

                //4.2: if got a signal then execute call
                if (state != null && validator.isExpect(state)) return state;

                //4.3: timeout test
                if (parker.isTimeout()) {
                    //4.3.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return false(abandon)
                    if (CasNodeUpdater.casNodeState(node, state, CasNodeState.TIMEOUT))
                        return validator.resultOnTimeout();
                } else if (state != null) {//3.4: reach here means not got expected value from call,then rest to continue waiting
                    node.setState(null);
                    Thread.yield();
                } else {//here: state == null
                    //4.5: park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, parker, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}


