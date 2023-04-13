/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import java.util.concurrent.TimeUnit;

/**
 * Task Config
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeTaskConfig {
    private final BeeTask task;
    private BeeTaskCallback callback;

    //time scheduled config
    private long initDelayTime;
    private long periodTime;
    private long fixedRateDelay;
    private TimeUnit timeUnit;

    public BeeTaskConfig(BeeTask task) {
        this.task = task;
    }

    public BeeTaskConfig(BeeTask task, BeeTaskCallback callback) {
        this.task = task;
        this.callback = callback;
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

    public long getInitDelayTime() {
        return initDelayTime;
    }

    public void setInitDelayTime(long initDelayTime) {
        this.initDelayTime = initDelayTime;
    }

    public long getPeriodTime() {
        return periodTime;
    }

    public void setPeriodTime(long periodTime) {
        this.periodTime = periodTime;
    }

    public long getFixedRateDelay() {
        return fixedRateDelay;
    }

    public void setFixedRateDelay(long fixedRateDelay) {
        this.fixedRateDelay = fixedRateDelay;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
