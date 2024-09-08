/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * A re-active worker
 *
 * @author Chris Liao
 * @version 1.0
 */

abstract class ReactivateWorker implements Runnable {
    protected static final AtomicIntegerFieldUpdater<ReactivateWorker> StateUpd = IntegerFieldUpdaterImpl.newUpdater(ReactivateWorker.class, "state");
    protected final PoolTaskCenter pool;

    protected volatile int state;
    protected Thread workThread;

    public ReactivateWorker(PoolTaskCenter pool) {
        this.pool = pool;
        this.state = WORKER_INACTIVE;
    }

    //@todo need more thinking here
    public void terminate() {
        int curState = this.state;
        if (curState == WORKER_INACTIVE) return;

        if (curState == WORKER_WAITING) {
            if (StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_INACTIVE)) {
                LockSupport.unpark(workThread);
            }
        }
    }

    public void wakeup() {
        int curState = state;
        if (curState == WORKER_INACTIVE) {
            if (StateUpd.compareAndSet(this, WORKER_INACTIVE, WORKER_STARTING)) {
                this.workThread = new Thread(this);
                this.state = WORKER_RUNNING;
                this.workThread.start();
            }
        } else if (curState == WORKER_WAITING) {
            if (StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_RUNNING)) {
                LockSupport.unpark(workThread);
            }
        }
    }
}
