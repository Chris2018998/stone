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
import org.stone.beetp.BeeTaskException;
import org.stone.beetp.BeeTaskScheduledHandle;

import static org.stone.beetp.BeeTaskStates.*;

/**
 * Scheduled task handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ScheduledTaskHandle extends BaseHandle implements BeeTaskScheduledHandle {
    private final long intervalTime;//nano seconds
    private final boolean fixedDelay;
    private long nextRunTime;//time sortable

    //only support periodic
    private int prevState;
    private long prevTime;
    private Object prevResult;

    //***************************************************************************************************************//
    //                1: constructor(1)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    ScheduledTaskHandle(BeeTask task, BeeTaskCallback callback,
                        long firstRunTime, long intervalTime, boolean fixedDelay, TaskExecutionPool pool) {
        super(task, callback, true, pool);
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
    public long getPrevTime() {
        return prevTime;
    }

    //value should be more than System.nanoTime(),when call done,then update time for next call
    public long getNextTime() {
        return nextRunTime;
    }

    //retrieve result of prev call
    public Object getPrevResult() throws BeeTaskException {
        if (!isPeriodic()) throw new BeeTaskException("Only support periodic schedule");
        if (prevState == TASK_CALL_RESULT) return prevResult;
        if (prevState == TASK_CALL_EXCEPTION) throw (BeeTaskException) prevResult;
        throw new BeeTaskException("Task not be called or not done until current");
    }

    //***************************************************************************************************************//
    //                              3: execute task                                                                  //
    //***************************************************************************************************************//
    void beforeExecute(TaskWorkThread workThread) {
        if (!isPeriodic()) pool.getTaskHoldingCount().decrementAndGet();
    }

    void afterExecute(TaskWorkThread workThread) {
        if (this.isPeriodic()) {
            this.prevState = this.state;
            this.prevResult = this.result;
            this.prevTime = this.nextRunTime;
            this.nextRunTime = intervalTime + (fixedDelay ? System.nanoTime() : nextRunTime);
            this.state = TASK_WAITING;//reset to waiting state for next call

            if (pool.getScheduledDelayedQueue().add(this) == 0)
                pool.wakeupSchedulePeekThread();
        } else {//one timed task,so end
            workThread.addCompletedCount();
        }
    }
}
