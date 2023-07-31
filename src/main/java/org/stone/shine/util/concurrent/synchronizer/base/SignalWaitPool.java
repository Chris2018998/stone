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
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.ThreadParkSupport;
import org.stone.shine.util.concurrent.synchronizer.ThreadWaitingPool;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.*;
import static org.stone.shine.util.concurrent.synchronizer.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Signal Wait Pool,caller try to get a signal from pool,if not get,then wait for it util timeout
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SignalWaitPool extends ThreadWaitingPool {

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param config thread wait config
     * @return true, if get a signal then return true,timeout return false
     * @throws InterruptedException exception from call or InterruptedException after thread park
     */
    public final boolean doWait(SyncVisitConfig config) throws InterruptedException {
        //1:check call parameter
        if (config == null) throw new IllegalArgumentException("wait config can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        //2:offer to wait queue
        SyncNode node = appendNode(config.getSyncNode());

        //3:get control parameters from config
        boolean allowInterrupted = config.supportInterrupted();
        ThreadParkSupport parkSupport = config.getParkSupport();

        //4: spin control（Logic from BeeCP）
        try {
            do {
                //4.1: read node state
                Object signal = node.getState();//any not null value regard as wakeup signal
                if (signal != null) {
                    if (signal == TIMEOUT) return false;
                    if (signal == INTERRUPTED) throw new InterruptedException();
                    return true;
                }

                //4.2: fail check
                if (parkSupport.isTimeout()) {
                    casState(node, signal, TIMEOUT);
                } else if (parkSupport.isInterrupted() && allowInterrupted) {
                    casState(node, signal, INTERRUPTED);
                } else if (parkSupport.computeParkNanos() > spinForTimeoutThreshold) {
                    parkSupport.park();
                }
            } while (true);
        } finally {
            //here:don't wakeup other
            this.leaveFromWaitQueue(node, false, true, node.getType(), RUNNING);
        }
    }
}
