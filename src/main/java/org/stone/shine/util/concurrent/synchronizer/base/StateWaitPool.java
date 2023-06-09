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

import org.stone.shine.util.concurrent.synchronizer.CasNode;
import org.stone.shine.util.concurrent.synchronizer.ThreadSpinPark;
import org.stone.shine.util.concurrent.synchronizer.ThreadWaitConfig;
import org.stone.shine.util.concurrent.synchronizer.ThreadWaitPool;
import org.stone.shine.util.concurrent.synchronizer.base.validator.ResultEqualsValidator;

import static org.stone.shine.util.concurrent.synchronizer.CasNodeUpdater.casState;
import static org.stone.shine.util.concurrent.synchronizer.CasStaticState.TIMEOUT;

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
        final CasNode node = config.getCasNode();
        if (config.isOutsideOfWaitPool()) super.appendNode(node);

        //2:get control parameters from config
        final boolean throwsIE = config.isAllowThrowsIE();
        final boolean wakeupOtherOnIE = config.isTransferSignalOnIE();
        final ThreadSpinPark parker = config.getThreadParkSupport();

        //3: spin control
        try {
            do {
                //3.1: read node state
                Object state = node.getState();

                //3.2: if got a signal then execute call
                if (state != null && validator.isExpected(state)) return state;

                //3.3: timeout test
                if (parker.isTimeout()) {
                    //3.3.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return false(abandon)
                    if (casState(node, state, TIMEOUT))
                        return validator.resultOnTimeout();
                } else if (state != null) {//3.4: reach here means not got expected value from call,then rest to continue waiting
                    node.setState(null);
                    Thread.yield();
                } else {//here: state == null
                    //3.5: park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, parker, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}


