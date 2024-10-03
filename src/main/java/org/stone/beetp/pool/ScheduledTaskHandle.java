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
import org.stone.beetp.pool.exception.TaskCancelledException;
import org.stone.beetp.pool.exception.TaskException;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * Scheduled task handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ScheduledTaskHandle<V> extends PoolTaskHandle<V> implements TaskScheduledHandle<V> {
    private final int hashCode;
    private final long intervalTime;//nano seconds
    private final boolean fixedDelay;
    private final TaskScheduleWorker scheduleWorker;
    private boolean cancelPending;

    private long executeTime;//time sortable
    private long lastExecutedTime;
    private Object lastExecutedState;
    private Object lastExecutedResult;

    //***************************************************************************************************************//
    //                1: constructor(1)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    ScheduledTaskHandle(Task<V> task, TaskAspect<V> callback,
                        long firstRunTime, long intervalTime, boolean fixedDelay, final TaskScheduleWorker scheduleWorker,
                        PoolTaskCenter pool) {
        super(task, callback, pool, true);

        this.executeTime = firstRunTime;
        this.intervalTime = intervalTime;
        this.fixedDelay = fixedDelay;
        this.scheduleWorker = scheduleWorker;

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

    //value should be more than System.nanoTime(),when call done,then update time for next call
    public long getNextTime() {
        return executeTime;
    }

    //nanoseconds(less than System.nanoTime())
    public long getLastTime() throws TaskException {
        if (lastExecutedState == null) throw new TaskException("Task has not  been executed");
        return lastExecutedTime;
    }

    public V getLastResult() throws TaskException {
        if (lastExecutedState == null) throw new TaskException("Task has not  been executed");
        if (lastExecutedState == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");
        if (lastExecutedState == TASK_EXCEPTIONAL) throw (TaskException) this.result;
        if (lastExecutedState == TASK_SUCCEED) return (V) this.lastExecutedResult;
        throw new TaskException("unknown last state");
    }

    //***************************************************************************************************************//
    //                                  3: cancel(1)                                                                 //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        //1: try to change state to cancelled
        if (state == TASK_WAITING && StateUpd.compareAndSet(this, TASK_WAITING, TASK_CANCELLED)) {
            this.fillTaskResult(TASK_CANCELLED, null);
            pool.decrementScheduledTaskCount();
            if (!this.scheduleWorker.remove(this) && taskBucket != null)
                taskBucket.remove(this);
            return true;//cancel successful
        }

        //2: interrupt process
        if (mayInterruptIfRunning) {//if set CANCELLED state on periodic task then not be scheduled for next execution
            Object curState = state;
            if (curState instanceof TaskExecutionWorker) {//in being executed
                this.cancelPending = true;//set cancelled state after this execution
                TaskExecutionWorker worker = (TaskExecutionWorker) curState;
                worker.interrupt();//thread interruption can't ensure process exit in time
            }
        }
        return false;
    }

    //***************************************************************************************************************//
    //                              4: execute task                                                                  //
    //***************************************************************************************************************//
    protected void afterExecute(boolean success, Object result) {
        this.lastExecutedResult = this.result;
        this.lastExecutedTime = this.executeTime;
        this.lastExecutedState = this.state;

        if (this.isPeriodic()) {
            if (cancelPending) {
                this.state = TASK_CANCELLED;
                pool.decrementScheduledTaskCount();
            } else {
                this.state = TASK_WAITING;
                this.executeTime = intervalTime + (fixedDelay ? System.nanoTime() : executeTime);
                scheduleWorker.put(this);
            }
        } else {//once task
            pool.decrementScheduledTaskCount();
        }
    }
}
