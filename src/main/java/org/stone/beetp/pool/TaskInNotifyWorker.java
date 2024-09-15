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

import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * A worker to wake up pool execution workers
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskInNotifyWorker extends ReactivateWorker {
    public TaskInNotifyWorker(PoolTaskCenter pool, long keepAliveTimeNanos, boolean useTimePark, int defaultSpins) {
        super(pool, keepAliveTimeNanos, useTimePark, defaultSpins);
    }

    public void run() {
        do {
            //@todo:wake target worker or all workers?
            for (TaskExecuteWorker worker : allWorkers)
                worker.wakeup();

            if (StateUpd.compareAndSet(this, WORKER_RUNNING, WORKER_WAITING)) {
                Thread.interrupted();//clear interrupted flag
                int resetState = WORKER_RUNNING;
                if (useTimePark) {
                    final long parkStartTime = System.nanoTime();
                    LockSupport.parkNanos(keepAliveTimeNanos);
                    if (System.nanoTime() - parkStartTime >= keepAliveTimeNanos) resetState = WORKER_INACTIVE;
                } else {
                    LockSupport.park();
                }

                //reset state
                if (state == WORKER_WAITING && StateUpd.compareAndSet(this, WORKER_WAITING, resetState) && resetState == WORKER_INACTIVE) {
                    break;
                }
            }
        } while (state != WORKER_INACTIVE);
    }
}
