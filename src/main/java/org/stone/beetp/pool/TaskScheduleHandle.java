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

/**
 * Task Schedule Handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskScheduleHandle extends TaskHandleImpl {
    private long executeTimePoint;

    private long periodNanoseconds;//==0,just once execute

    TaskScheduleHandle(BeeTask task, int initState, TaskExecutionPool pool) {
        super(task, initState, pool);
    }

    public long getExecuteTimePoint() {
        return executeTimePoint;
    }

    public void setExecuteTimePoint(long executeTimePoint) {
        this.executeTimePoint = executeTimePoint;
    }

    public long getPeriodNanoseconds() {
        return periodNanoseconds;
    }

    public void setPeriodNanoseconds(long periodNanoseconds) {
        this.periodNanoseconds = periodNanoseconds;
    }
}
