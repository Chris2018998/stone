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

/**
 * Result call executed inside pool and compare result of call with expected parameter,if equals,return true
 * and leave from pool,not equals,then join in wait queue util wakeup from other and execute call again and compare.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ResultWaitPool extends ThreadWaitPool {
    //true,use fair mode to execute call
    private boolean fair;

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public ResultWaitPool() {
    }

    public ResultWaitPool(boolean fair) {
        this.fair = fair;
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
     * @param expect compare to the call result
     * @return true, call result equaled to the expect parameter,false wait timeout in pool
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final boolean doCall(ResultCall call, Object arg, Object expect, ThreadWaitConfig config) throws Exception {
        //1:check call parameter
        if (call == null) throw new IllegalArgumentException("call can't be null");
        if (config == null) throw new IllegalArgumentException("wait config can't be null");

        //2:try to execute call
        if (fair) { //fair mode
            if (!this.hasQueuedThreads() && equals(call.call(arg), expect)) return true;
        } else if (equals(call.call(arg), expect)) return true;

        //3:create wait node and offer to wait queue
        ThreadNode node = config.getThreadNode();
        if (config.isNeedAddWaitPool()) super.appendNode(config.getThreadNode());

        //4:get control parameters from config
        boolean throwsIE = config.isThrowsIE();
        boolean wakeupOtherOnIE = config.isTransferSignalOnIE();

        //5:create thread parker
        ThreadParkSupport parker = config.getThreadParkSupport();

        //6:spin control
        try {
            do {
                //6.1:if got a signal then execute call
                if (equals(call.call(arg), expect)) return true;

                //6.2:read node state
                Object state = node.getState();

                //6.3:timeout test
                if (parker.isTimeout()) {
                    //6.3.1:try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return false(abandon)
                    if (ThreadNodeUpdater.casNodeState(node, state, ThreadNodeState.TIMEOUT)) return false;
                } else if (state != null) {//6.4:reach here means not got expected value from call,then rest to continue waiting
                    node.setState(null);
                    Thread.yield();
                } else {//here: state == null
                    //6.5:park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, parker, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            if (config.isNeedRemoveOnLeave()) super.removeNode(node);
        }
    }
}
