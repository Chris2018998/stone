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
    protected final int defaultSpins;
    protected final boolean useTimePark;
    protected final long keepAliveTimeNanos;

    protected volatile int state;
    protected Thread workThread;

    public ReactivateWorker(long keepAliveTimeNanos, boolean useTimePark, int defaultSpins) {
        this.state = WORKER_PASSIVATED;
        this.useTimePark = useTimePark;
        this.defaultSpins = defaultSpins;
        this.keepAliveTimeNanos = keepAliveTimeNanos;
    }

    /**
     * Query worker state is whether in running
     *
     * @return true that worker is running
     */
    public boolean isRunning() {
        return state == WORKER_RUNNING;
    }

    //activate this worker to work
    public void activate() {
        do {
            int curState = state;
            if (curState == WORKER_RUNNING) break;

            if (curState == WORKER_WAITING) {
                if (StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_RUNNING)) {
                    LockSupport.unpark(workThread);
                    return;
                }
            } else if (curState == WORKER_PASSIVATED) {
                if (StateUpd.compareAndSet(this, WORKER_PASSIVATED, WORKER_ACTIVATING)) {
                    this.workThread = new Thread(this);
                    this.state = WORKER_RUNNING;
                    this.workThread.start();
                    return;
                }
            }
        } while (true);
    }

    /**
     * passivate this worker and make it to be in passivated state
     *
     * @param mayInterruptIfRunning is true then attempt to interrupt worker thread if in blocking
     * @return true when successful;otherwise return false
     */
    public boolean passivate(boolean mayInterruptIfRunning) {
        do {
            int curState = this.state;
            if (curState == WORKER_PASSIVATED) return false;

            if (curState != WORKER_ACTIVATING && StateUpd.compareAndSet(this, curState, WORKER_PASSIVATED)) {
                if (curState == WORKER_WAITING) {
                    LockSupport.unpark(workThread);
                } else if (mayInterruptIfRunning) {
                    workThread.interrupt();
                }
                return true;
            }
        } while (true);
    }

    /**
     * interrupt this worker
     */
    public void interrupt() {
        do {
            int curState = state;
            if (curState == WORKER_PASSIVATED || curState == WORKER_ACTIVATING) return;

            if (curState == WORKER_WAITING) {
                LockSupport.unpark(workThread);
                return;
            } else if (curState == WORKER_RUNNING) {
                this.workThread.interrupt();
                return;
            }
        } while (true);
    }
}
