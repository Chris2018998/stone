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

import org.stone.beetp.TaskPoolMonitorVo;

/**
 * execution Monitor Vo
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class PoolMonitorVo implements TaskPoolMonitorVo {
    private int poolState;
    private int taskCount;
    private int taskRunningCount;
    private long taskCompletedCount;
    private int scheduledTaskCount;

    public int getPoolState() {
        return poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getTaskCount() {
        return taskCount;
    }

    void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getRunningCount() {
        return taskRunningCount;
    }

    void setTaskRunningCount(int taskRunningCount) {
        this.taskRunningCount = taskRunningCount;
    }

    public long getCompletedCount() {
        return taskCompletedCount;
    }

    void setTaskCompletedCount(long taskCompletedCount) {
        this.taskCompletedCount = taskCompletedCount;
    }

    public int getScheduledTaskCount() {
        return scheduledTaskCount;
    }

    void setScheduledTaskCount(int scheduledTaskCount) {
        this.scheduledTaskCount = scheduledTaskCount;
    }
}
