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
    //true,use fair mode to execute action
    private boolean fair;

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public ResultWaitPool() {
    }

    public ResultWaitPool(boolean fair) {
        this.fair = fair;
    }

    public boolean isFair() {
        return this.fair;
    }

    //****************************************************************************************************************//
    //                                          2: call method(3)                                                     //
    //****************************************************************************************************************//

    /**
     * execute the call inside pool and compare its result with expected parameter value,if equivalence that this
     * method return true,not equals that wait timeout and return false.
     *
     * @param call     executed in pool to get result
     * @param arg      call argument
     * @param expect   compare to the call result
     * @param support  thread park support
     * @param throwsIE true,throws InterruptedException when interrupted
     * @return true, call result equals to the expect parameter,false wait timeout in pool
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final boolean doCall(ResultCall call, Object arg, Object expect, ThreadParkSupport support, boolean throwsIE) throws Exception {
        return doCall(call, arg, expect, support, throwsIE, null);
    }

    /**
     * execute the call inside pool and compare its result with expected parameter value,if equivalence that this
     * method return true,not equals that wait timeout and return false.
     *
     * @param call      executed in pool to get result
     * @param arg       call argument
     * @param expect    compare to the call result
     * @param support   thread park support
     * @param throwsIE  true,throws InterruptedException when interrupted
     * @param nodeValue property of wait node
     * @return true, call result equals to the expect parameter,false wait timeout in pool
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final boolean doCall(ResultCall call, Object arg, Object expect, ThreadParkSupport support, boolean throwsIE, Object nodeValue) throws Exception {
        //1:check call parameter
        if (call == null) throw new IllegalArgumentException("call can't be null");

        //2:execute call
        if (fair) { //fair mode
            if (!this.hasQueuedThreads() && equals(call.call(arg), expect)) return true;
        } else if (equals(call.call(arg), expect)) return true;

        //3:call inner method
        return doCallByNode(call, arg, expect, support, throwsIE, createNode(nodeValue));
    }

    /**
     * execute the call inside pool and compare its result with expected parameter value,if equivalence that this
     * method return true,not equals that wait timeout and return false.
     *
     * @param call     executed in pool to get result
     * @param arg      call argument
     * @param expect   compare to the call result
     * @param support  thread park support
     * @param throwsIE true,throws InterruptedException when interrupted
     * @param node     preCreated wait node(for example: nodes wait in lock condition queue,at finally,them need removed and offered to syn queue to get lock)
     * @return boolean value,true means wakeup with expect signal state,false,wait timeout
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final boolean doCallByNode(ResultCall call, Object arg, Object expect, ThreadParkSupport support, boolean throwsIE, ThreadNode node) throws Exception {
        //1:check call parameter
        if (call == null) throw new IllegalArgumentException("call can't be null");
        if (node == null) throw new IllegalArgumentException("wait node can't be null");

        //2:add node to queue
        super.appendNode(node);

        //3:spin control
        try {
            do {
                //3.1: read node state
                Object state = node.getState();

                //3.2:if got a signal then execute call
                if (state != null && equals(call.call(arg), expect)) return true;

                //3.3: timeout test
                if (support.isTimeout()) {
                    //3.3.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return false(abandon)
                    if (ThreadNodeUpdater.casNodeState(node, state, ThreadNodeState.TIMEOUT)) return false;
                } else if (state != null) {//3.4: reach here means not got expected value from call,then rest to continue waiting
                    node.setState(null);
                    Thread.yield();
                    //jump to next read
                } else {//here: state == null
                    //3.5: park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, support, throwsIE, true);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
