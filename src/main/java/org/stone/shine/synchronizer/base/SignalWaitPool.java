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
 * Signal Wait Pool,caller expect to get a signal from pool,if not get,then wait for it util timeout
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SignalWaitPool extends ThreadWaitPool {

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param throwsIE true if interrupted during waiting then throw exception{@link InterruptedException},false,ignore interruption
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final boolean doWait(ThreadParkSupport support, boolean throwsIE) throws InterruptedException {
        return doWait(support, throwsIE, super.createNode(null), true);
    }

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param throwsIE  true if interrupted during waiting then throw exception{@link InterruptedException},false,ignore interruption
     * @param nodeValue a property of wait node and can be regarded as node waiting type,and using in some wakeup methods
     * @return true that the caller got a signal from other,false that the caller wait timeout in pool
     * @throws InterruptedException caller waiting interrupted,then throws it
     */
    public final boolean doWait(ThreadParkSupport support, boolean throwsIE, Object nodeValue) throws InterruptedException {
        return doWait(support, throwsIE, super.createNode(nodeValue), true);
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
    public final boolean doWait(ThreadParkSupport support, boolean throwsIE, Object nodeValue, boolean wakeupOtherOnIE) throws InterruptedException {
        return doWait(support, throwsIE, super.createNode(nodeValue), wakeupOtherOnIE);
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
    public final boolean doWait(ThreadParkSupport support, boolean throwsIE, ThreadNode node, boolean wakeupOtherOnIE) throws InterruptedException {
        //1:create wait node and offer to wait queue
        super.appendNode(node);

        //2:spin control
        try {
            do {
                //2.1: read node state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) return true;

                //2.2: timeout test
                if (support.isTimeout()) {
                    //2.2.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return false
                    if (ThreadNodeUpdater.casNodeState(node, null, ThreadNodeState.TIMEOUT)) return false;
                } else {
                    //2.3: park current thread(lock condition need't wakeup other waiters in condition queue,because all waiters will move to syn queue)
                    parkNodeThread(node, support, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
