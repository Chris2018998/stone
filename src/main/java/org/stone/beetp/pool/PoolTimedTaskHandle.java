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

/**
 * Scheduled task handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class PoolTimedTaskHandle<V> extends PoolTaskHandle<V> implements TaskScheduledHandle<V> {
    private final long intervalTime;//nano seconds
    private final boolean fixedDelay;
    private final long nextRunTime;//time sortable

    //only support periodic
    private int prevState;
    private long prevTime;
    private Object prevResult;

    //***************************************************************************************************************//
    //                1: constructor(1)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    PoolTimedTaskHandle(Task<V> task, TaskAspect<V> callback,
                        long firstRunTime, long intervalTime, boolean fixedDelay, PoolTaskCenter pool) {
        super(task, callback, pool, true);
        this.nextRunTime = firstRunTime;//first run time
        this.intervalTime = intervalTime;
        this.fixedDelay = fixedDelay;//true:calculate next run time from task prev call end t
    }

    //***************************************************************************************************************//
    //                2: impl interface methods(5)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public boolean isPeriodic() {
        return intervalTime != 0;
    }

    public boolean isFixedDelay() {
        return fixedDelay;
    }

    //nanoseconds(less than System.nanoTime())
    public long getLastTime() {
        return prevTime;
    }

    //value should be more than System.nanoTime(),when call done,then update time for next call
    public long getNextTime() {
        return nextRunTime;
    }

    //retrieve result of prev call
    public V getLastResult() throws TaskException {
//        if (!isPeriodic()) throw new TaskException("Only support periodic schedule");
//        if (prevState == TASK_COMPLETED) return (V) prevResult;
//        if (prevState == TASK_FAILED) throw (TaskException) prevResult;
//        throw new TaskException("Task not be called or not done until current");

        return null;
    }

    //***************************************************************************************************************//
    //                              3: execute task                                                                  //
    //***************************************************************************************************************//
    protected void beforeExecute() {
        //@todo to be implemented
        //if (!isPeriodic()) pool.getTaskCount().decrementAndGet();
    }

    private void afterExecute(TaskExecuteWorker worker) {
        //@todo to be implemented

//        if (this.isPeriodic()) {
//            this.prevState = this.state;
//            this.prevResult = this.result;
//            this.prevTime = this.nextRunTime;
//            this.nextRunTime = intervalTime + (fixedDelay ? System.nanoTime() : nextRunTime);
//            this.state = TASK_WAITING;//reset to waiting state for next call
//
//            if (pool.getScheduledDelayedQueue().add(this) == 0)
//                pool.wakeupSchedulePeekThread();
//        } else {//one timed task,so end
//            worker.completedCount++;
//        }
    }
}
