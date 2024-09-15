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
    protected final int defaultSpins;
    protected final boolean useTimePark;
    protected final long keepAliveTimeNanos;
    protected final TaskExecuteWorker[] allWorkers;

    protected volatile int state;
    protected Thread workThread;

    public ReactivateWorker(PoolTaskCenter pool, long keepAliveTimeNanos, boolean useTimePark, int defaultSpins) {
        this.pool = pool;
        this.state = WORKER_INACTIVE;

        this.useTimePark = useTimePark;
        this.defaultSpins = defaultSpins;
        this.keepAliveTimeNanos = keepAliveTimeNanos;
        this.allWorkers = pool.getExecuteWorkers();
    }

    /**
     * interrupt this worker
     */
    public void interrupt() {
        if (workThread != null) workThread.interrupt();
    }

    /**
     * active this worker to run
     */
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

    /**
     * terminate this worker and make it to be in inactive state
     *
     * @param mayInterruptIfRunning is true then attempt to interrupt worker thread if in blocking
     * @return true when successful;otherwise return false
     */
    public boolean terminate(boolean mayInterruptIfRunning) {
        do {
            int curState = this.state;
            if (curState == WORKER_INACTIVE) return false;

            if (curState == WORKER_WAITING) {
                if (StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_INACTIVE)) {
                    LockSupport.unpark(workThread);
                    return true;
                }
            } else if (mayInterruptIfRunning) {
                Thread.State threadState = workThread.getState();
                if (threadState == Thread.State.WAITING || threadState == Thread.State.TIMED_WAITING) {
                    workThread.interrupt();
                    return workThread.isInterrupted();
                }
            } else {
                return false;
            }
        } while (true);
    }
}
