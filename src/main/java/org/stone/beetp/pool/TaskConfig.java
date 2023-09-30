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

    //methods block of scheduled task
    private int scheduledType;
    private TimeUnit timeUnit;
    private long initialDelay;
    private long intervalDelay;
    private boolean fixedDelay;

    TaskConfig(BeeTask task) {
        this.task = task;
    }

    //***************************************************************************************************************//
    //                 1: methods block of once task                                                                 //                                                                                  //
    //***************************************************************************************************************//
    BeeTask getTask() {
        return task;
    }

    BeeTaskCallback getCallback() {
        return callback;
    }

    void setCallback(BeeTaskCallback callback) {
        this.callback = callback;
    }

    //***************************************************************************************************************//
    //                 2: methods block of join task                                                                 //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskJoinOperator getJoinOperator() {
        return operator;
    }

    void setJoinOperator(BeeTaskJoinOperator operator) {
        this.operator = operator;
    }

    //***************************************************************************************************************//
    //                3: methods block of Scheduled task                                                             //                                                                                  //
    //***************************************************************************************************************//
    int getScheduledType() {
        return scheduledType;
    }

    void setScheduledType(int scheduledType) {
        this.scheduledType = scheduledType;
    }

    TimeUnit getTimeUnit() {
        return timeUnit;
    }

    void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    long getInitialDelay() {
        return initialDelay;
    }

    void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    long getIntervalDelay() {
        return intervalDelay;
    }

    void setIntervalDelay(long intervalDelay) {
        this.intervalDelay = intervalDelay;
    }

    boolean isFixedDelay() {
        return fixedDelay;
    }

    void setFixedDelay(boolean fixedDelay) {
        this.fixedDelay = fixedDelay;
    }
}
