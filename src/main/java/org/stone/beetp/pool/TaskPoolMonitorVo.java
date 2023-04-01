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
    private int workerCount;
    private int queueTaskCount;
    private int runningTaskCount;
    private int completedTaskCount;

    public int getWorkerCount() {
        return workerCount;
    }

    void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public int getQueueTaskCount() {
        return queueTaskCount;
    }

    void setQueueTaskCount(int queueTaskCount) {
        this.queueTaskCount = queueTaskCount;
    }

    public int getRunningTaskCount() {
        return runningTaskCount;
    }

    void setRunningTaskCount(int runningTaskCount) {
        this.runningTaskCount = runningTaskCount;
    }

    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    void setCompletedTaskCount(int completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }
}
