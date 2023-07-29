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

import org.stone.shine.util.concurrent.synchronizer.SyncNode;
import org.stone.shine.util.concurrent.synchronizer.SyncWakeupPool;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.TIMEOUT;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casState;

/**
 * Signal Wait Pool,caller try to get a signal from pool,if not get,then wait for it util timeout
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SignalWaitPool extends SyncWakeupPool {

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param config thread wait config
     * @return true, if get a signal then return true,timeout return false
     * @throws InterruptedException exception from call or InterruptedException after thread park
     */
    public final boolean doWait(ThreadSpinConfig config) throws InterruptedException {
        //1:check call parameter
        if (config == null) throw new IllegalArgumentException("wait config can't be null");

        //2:create wait node and offer to wait queue
        final SyncNode node = config.getCasNode();
        if (config.isOutsideOfWaitPool()) super.appendNode(node);

        //3:get control parameters from config
        final boolean throwsIE = config.isAllowThrowsIE();
        final boolean wakeupOtherOnIE = config.isTransferSignalOnIE();
        final ThreadSpinParker parker = config.getThreadParkSupport();

        //4:spin control
        try {
            do {
                //4.1: read node state
                Object state = node.getState();//any not null value regard as wakeup signal
                if (state != null) return true;

                //4.2: timeout test
                if (parker.isTimeout()) {
                    //4.2.1: try cas state from null to TIMEOUT(more static states,@see{@link ThreadNodeState})then return false
                    if (casState(node, null, TIMEOUT)) return false;
                } else {
                    //4.3: park current thread(lock condition need't wakeup other waiters in condition queue,because all waiters will move to syn queue)
                    parkNodeThread(node, parker, throwsIE, wakeupOtherOnIE);
                }
            } while (true);
        } finally {
            super.removeNode(node);
        }
    }
}
