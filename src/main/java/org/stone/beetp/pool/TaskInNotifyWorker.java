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
            for (TaskExecuteWorker worker : allWorkers)
                worker.wakeup();

            this.state = WORKER_WAITING;
            boolean parkTimeout = false;
            if (useTimePark) {
                long parkStartTime = System.nanoTime();
                LockSupport.parkNanos(keepAliveTimeNanos);
                parkTimeout = System.nanoTime() - parkStartTime >= keepAliveTimeNanos;
            } else {
                LockSupport.park();
            }

            StateUpd.compareAndSet(this, WORKER_WAITING, parkTimeout ? WORKER_INACTIVE : WORKER_RUNNING);
        } while (state != WORKER_INACTIVE);
    }
}
