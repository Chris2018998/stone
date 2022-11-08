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
 * wait expected state
 *
 * @author Chris Liao
 * @version 1.0
 */

public class StateWaitPool extends ThreadWaitPool {

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param throwsIE true if interrupted during waiting then throw exception{@link InterruptedException},false,ignore interruption
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final boolean doWait(Object expect, ThreadParkSupport support, boolean throwsIE) throws InterruptedException {
        return doWait(expect, support, throwsIE, super.createNode(null), true);
    }

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param throwsIE  true if interrupted during waiting then throw exception{@link InterruptedException},false,ignore interruption
     * @param nodeValue a property of wait node and can be regarded as node waiting type,and using in some wakeup methods
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final boolean doWait(Object expect, ThreadParkSupport support, boolean throwsIE, Object nodeValue) throws InterruptedException {
        return doWait(expect, support, throwsIE, super.createNode(nodeValue), true);
    }

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param throwsIE        true if interrupted during waiting then throw exception{@link InterruptedException},false,ignore interruption
     * @param nodeValue       a property of wait node and can be regarded as node waiting type,and using in some wakeup methods
     * @param wakeupOtherOnIE true,if interrupted and has got a signal,then transfer the signal to another waiter
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final boolean doWait(Object expect, ThreadParkSupport support, boolean throwsIE, Object nodeValue, boolean wakeupOtherOnIE) throws InterruptedException {
        return doWait(expect, support, throwsIE, super.createNode(nodeValue), wakeupOtherOnIE);
    }

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param throwsIE        true if interrupted during waiting then throw exception{@link InterruptedException},false,ignore interruption
     * @param node            preCreated thread wait node
     * @param wakeupOtherOnIE true,if interrupted and has got a signal,then transfer the signal to another waiter
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final boolean doWait(Object expect, ThreadParkSupport support, boolean throwsIE, ThreadNode node, boolean wakeupOtherOnIE) throws InterruptedException {
        if (expect == null) throw new IllegalArgumentException("expect state can't be null");

        //1:create wait node and offer to wait queue
        super.appendNode(node);

        //2:spin control
        try {
            do {
                //3.1: read node state
                Object state = node.getState();

                //3.2:if got a signal then execute call
                if (state != null && equals(state, expect)) return true;

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
                    parkNodeThread(node, support, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}


