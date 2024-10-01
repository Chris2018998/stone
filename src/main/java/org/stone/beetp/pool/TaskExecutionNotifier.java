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

import org.stone.beetp.TaskPoolThreadFactory;

import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * A worker to activate all execution workers in pool
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskExecutionNotifier extends PoolBaseWorker {
    private final TaskExecutionWorker[] workers;

    public TaskExecutionNotifier(TaskPoolThreadFactory threadFactory,
                                 long keepAliveTimeNanos, boolean useTimePark, int defaultSpins, TaskExecutionWorker[] workers) {
        super(threadFactory, keepAliveTimeNanos, useTimePark, defaultSpins);
        this.workers = workers;
    }

    public void run() {
        do {
            for (TaskExecutionWorker worker : workers)
                worker.activate();

            if (StateUpd.compareAndSet(this, WORKER_RUNNING, WORKER_WAITING)) {
                int resetState = WORKER_RUNNING;
                Thread.interrupted();//clear interrupted flag
                if (useTimePark) {
                    final long parkStartTime = System.nanoTime();
                    LockSupport.parkNanos(keepAliveTimeNanos);
                    if (System.nanoTime() - parkStartTime >= keepAliveTimeNanos) resetState = WORKER_PASSIVATED;
                } else {
                    LockSupport.park();
                }

                //reset state
                if (state == WORKER_WAITING && StateUpd.compareAndSet(this, WORKER_WAITING, resetState) && resetState == WORKER_PASSIVATED) {
                    break;
                }
            }
        } while (state != WORKER_PASSIVATED);
    }
}
