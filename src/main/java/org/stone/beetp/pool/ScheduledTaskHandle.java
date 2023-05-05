/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.pool;

import org.stone.beetp.BeeTask;
import org.stone.beetp.BeeTaskCallback;

import static org.stone.beetp.pool.PoolStaticCenter.TASK_SCHEDULING;

/**
 * Task Schedule Handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ScheduledTaskHandle extends GenericTaskHandle {
    private long nextExecutionTime;
    private long delayNanoseconds;
    private boolean delayFromExecuteTime;

    ScheduledTaskHandle(BeeTask task, int state, BeeTaskCallback callback, TaskExecutionPool pool) {
        super(task, state, callback, pool);
    }

    void setScheduledTime(long nextExecutionTime, long delayNanoseconds, boolean delayFromExecuteTime) {
        this.nextExecutionTime = nextExecutionTime;
        this.delayNanoseconds = delayNanoseconds;
        this.delayFromExecuteTime = delayFromExecuteTime;
    }

    //Override method
    int getReadyStateBeforeRunning() {
        return TASK_SCHEDULING;
    }

    long getNextExecutionTime() {
        return nextExecutionTime;
    }

    //calculate next execution time(Nanoseconds)
    long calculateNextExecutionTime() {
        if (delayNanoseconds > 0) {//periodic
            long startTime = delayFromExecuteTime ? nextExecutionTime : System.nanoTime();
            while (nextExecutionTime <= System.nanoTime()) {
                nextExecutionTime = startTime + delayNanoseconds;
            }
        }
        return nextExecutionTime;
    }
}
