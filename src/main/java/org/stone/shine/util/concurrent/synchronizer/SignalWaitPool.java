/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer;

import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;

import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeStates.REMOVED;
import static org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater.casState;
import static org.stone.tools.CommonUtil.maxTimedSpins;

/**
 * Signal-ObjectWaitPool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class SignalWaitPool extends ObjectWaitPool {

    /**
     * try to get a signal from pool,if not get,then wait until a wakeup signal or wait timeout.
     *
     * @param config thread wait config
     * @return true, if get a signal then return true,timeout return false
     * @throws java.lang.InterruptedException exception from call or InterruptedException after thread park
     */
    public final Object get(SyncVisitConfig config) throws InterruptedException {
        //1:check call parameter
        if (config == null) throw new NullPointerException("Sync config can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        //2:offer to wait queue
        SyncNode node = config.getSyncNode();
        int spins = appendAsWaitNode(node) ? maxTimedSpins : 0;//spin count

        //3:get control parameters from config
        ThreadParkSupport parkSupport = config.getParkSupport();

        //4: spin control（Logic from BeeCP）
        try {
            do {
                //4.1: read node state
                Object signal = node.getState();//any not null value regard as wakeup signal
                if (signal != null) return signal;

                //4.2: fail check
                if (parkSupport.isTimeout()) {
                    if (casState(node, null, REMOVED)) return false;
                } else if (parkSupport.isInterrupted() && config.isAllowInterruption()) {
                    if (casState(node, null, REMOVED)) throw new InterruptedException();
                } else if (spins > 0) {
                    --spins;
                } else {
                    parkSupport.computeAndPark();
                }
            } while (true);
        } finally {
            waitQueue.remove(node);
        }
    }
}
