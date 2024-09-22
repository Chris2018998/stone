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

import org.stone.beetp.Task;
import org.stone.beetp.TaskAspect;
import org.stone.beetp.TaskScheduledHandle;
import org.stone.beetp.pool.exception.TaskException;

import static org.stone.beetp.pool.PoolConstants.TASK_WAITING;

/**
 * Scheduled task handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class PoolTimedTaskHandle<V> extends PoolTaskHandle<V> implements TaskScheduledHandle<V> {
    private final int hashCode;
    private final long intervalTime;//nano seconds
    private final boolean fixedDelay;
    private long executeTime;//time sortable

    private long lastExecutedTime;
    private Object lastExecutedState;
    private Object lastExecutedResult;

    //***************************************************************************************************************//
    //                1: constructor(1)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    PoolTimedTaskHandle(Task<V> task, TaskAspect<V> callback,
                        long firstRunTime, long intervalTime, boolean fixedDelay, PoolTaskCenter pool) {
        super(task, callback, pool, true);

        this.executeTime = firstRunTime;
        this.intervalTime = intervalTime;
        this.fixedDelay = fixedDelay;

        int hashCode = super.hashCode();
        this.hashCode = hashCode ^ (hashCode >>> 16);
    }

    //***************************************************************************************************************//
    //                2: impl interface methods(5)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public int hashCode() {
        return hashCode;
    }

    public boolean isPeriodic() {
        return intervalTime != 0;
    }

    public boolean isFixedDelay() {
        return fixedDelay;
    }

    //nanoseconds(less than System.nanoTime())
    public long getLastTime() {
        return lastExecutedTime;
    }

    //value should be more than System.nanoTime(),when call done,then update time for next call
    public long getNextTime() {
        return executeTime;
    }

    //retrieve result of last call
    public V getLastResult() throws TaskException {


        return (V) lastExecutedResult;
    }

    //***************************************************************************************************************//
    //                              3: execute task                                                                  //
    //***************************************************************************************************************//
    protected void afterExecute(boolean success, Object result) {
        if (this.isPeriodic()) {
            this.lastExecutedState = this.state;
            this.lastExecutedResult = this.result;
            this.lastExecutedTime = this.executeTime;

            this.state = TASK_WAITING;
            this.executeTime = intervalTime + (fixedDelay ? System.nanoTime() : executeTime);
            taskBucket.put(this);
        } else {
            pool.decrementTimedTaskCount();
        }
    }
}
