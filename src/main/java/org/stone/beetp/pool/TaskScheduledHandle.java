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

import static org.stone.beetp.pool.TaskPoolStaticUtil.*;

/**
 * Schedule Handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskScheduledHandle extends TaskExecuteHandle implements BeeTaskScheduledHandle {
    private long nextRunTime;//time sortable
    private long delayTime;
    private boolean fixedDelay;

    //only support periodic
    private int prevState;
    private long prevCallTime;
    private Object prevCallResult;

    //***************************************************************************************************************//
    //                1: constructor(1)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    TaskScheduledHandle(BeeTask task, BeeTaskCallback callback, TaskExecutionPool pool,
                        long nextRunTime, long delayTime, boolean fixedDelay) {
        super(task, callback, pool);
        this.nextRunTime = nextRunTime;//first run time
        this.delayTime = delayTime;
        this.fixedDelay = fixedDelay;//true:calculate next run time from task prev call end t
    }

    //***************************************************************************************************************//
    //                2: impl interface methods(6)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public boolean isPeriodic() {
        return delayTime != 0;
    }

    public boolean isFixedDelay() {
        return fixedDelay;
    }

    //nanoseconds(less than System.nanoTime())
    public long getPrevCallTime() {
        return prevCallTime;
    }

    //value should be more than System.nanoTime(),when call done,then update time for next call
    public long getNextCallTime() {
        return nextRunTime;
    }

    //retrieve result of prev call
    public Object getPrevCallResult() throws BeeTaskException {
        if (!isPeriodic()) throw new BeeTaskException("Just support periodic schedule");
        if (prevState == TASK_RESULT) return prevCallResult;
        if (prevState == TASK_EXCEPTION) throw (BeeTaskException) prevCallResult;
        throw new BeeTaskException("Task not be called or not done until current");
    }

    //***************************************************************************************************************//
    //                3: preparation for next call(periodic)                                                         //
    //***************************************************************************************************************//
    //true:re-offer to array after this method call
    boolean prepareForNextCall() {
        if (isPeriodic()) {
            this.prevState = this.curState.get();
            this.prevCallResult = this.curResult;
            this.prevCallTime = this.nextRunTime;

            long startTime = fixedDelay ? System.nanoTime() : nextRunTime;
            this.nextRunTime = calculateNextRunTime(startTime, delayTime);
            this.curState.set(TASK_WAITING);
            return true;
        } else {
            return false;
        }
    }
}
