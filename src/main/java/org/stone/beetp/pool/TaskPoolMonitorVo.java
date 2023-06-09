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

import org.stone.beetp.BeeTaskPoolMonitorVo;

/**
 * pool Monitor Vo
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class TaskPoolMonitorVo implements BeeTaskPoolMonitorVo {
    private int poolState;
    private int workerCount;
    private int taskHoldingCount;
    private long taskRunningCount;
    private long taskCompletedCount;

    public int getPoolState() {
        return poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public int getTaskHoldingCount() {
        return taskHoldingCount;
    }

    void setTaskHoldingCount(int taskHoldingCount) {
        this.taskHoldingCount = taskHoldingCount;
    }

    public long getTaskRunningCount() {
        return taskRunningCount;
    }

    void setTaskRunningCount(long taskRunningCount) {
        this.taskRunningCount = taskRunningCount;
    }

    public long getTaskCompletedCount() {
        return taskCompletedCount;
    }

    void setTaskCompletedCount(long taskCompletedCount) {
        this.taskCompletedCount = taskCompletedCount;
    }
}
