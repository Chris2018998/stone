/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.synchronizer.base;

import org.stone.shine.synchronizer.ThreadNode;
import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.ThreadWaitPool;

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
                //3.1:execute resultCall
                if (equals(call.call(arg), expect)) return true;
                if (node.getState() != null) {//any not null value regard as wakeup signal
                    node.setState(null);
                    Thread.yield();
                }

                //3.2:park current thread
                parkNodeThread(node, support, throwsIE);
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
