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
    private long runningTaskCount;
    private long completedTaskCount;

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

    public long getRunningTaskCount() {
        return runningTaskCount;
    }

    void setRunningTaskCount(long runningTaskCount) {
        this.runningTaskCount = runningTaskCount;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }
}
