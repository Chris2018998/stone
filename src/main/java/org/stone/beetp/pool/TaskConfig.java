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
import org.stone.beetp.BeeTaskJoinOperator;

import java.util.concurrent.TimeUnit;

/**
 * Task Config
 *
 * @author Chris Liao
 * @version 1.0
 */
final class TaskConfig {
    private final BeeTask task;
    private BeeTaskCallback callback;
    private BeeTaskJoinOperator operator;

    private int scheduleType;
    private TimeUnit timeUnit;
    private long initialDelay;
    private long intervalDelay;
    private boolean fixedDelay;

    TaskConfig(BeeTask task) {
        this.task = task;
    }

    public BeeTask getTask() {
        return task;
    }

    public BeeTaskCallback getCallback() {
        return callback;
    }

    public void setCallback(BeeTaskCallback callback) {
        this.callback = callback;
    }

    public BeeTaskJoinOperator getOperator() {
        return operator;
    }

    public void setOperator(BeeTaskJoinOperator operator) {
        this.operator = operator;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public long getIntervalDelay() {
        return intervalDelay;
    }

    public void setIntervalDelay(long intervalDelay) {
        this.intervalDelay = intervalDelay;
    }

    public boolean isFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(boolean fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public int getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(int scheduleType) {
        this.scheduleType = scheduleType;
    }
}
