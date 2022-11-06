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
 * get result from call and compare with expected value,if true,then leave from pool
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
     * execute result call
     *
     * @param call     plugin call action
     * @param arg      call arguments
     * @param expect   compare to the call result
     * @param support  thread park support
     * @param throwsIE true,throws InterruptedException when interrupted
     * @return boolean value,true means wakeup with expect signal state,false,wait timeout
     * @throws Exception exception from call or InterruptedException after thread park
     */
    public final boolean doCall(ResultCall call, Object arg, Object expect, ThreadParkSupport support, boolean throwsIE) throws Exception {
        return doCall(call, arg, expect, support, throwsIE, null);
    }

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

    //do resultCall with node(used in lock-condition queue?)
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
                } else {//3.4: park the node thread
                    if (state != null) {//3.4.1: reach here means not got expected value from call,then rest to continue waiting
                        node.setState(null);
                        Thread.yield();
                    }

                    //3.4.2: park current thread(if interrupted then transfer the got state value to another waiter)
                    parkNodeThread(node, support, throwsIE, true);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
