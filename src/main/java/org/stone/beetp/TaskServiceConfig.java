/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

import org.stone.beetp.pool.PoolTaskCenter;
import org.stone.beetp.pool.exception.TaskServiceConfigException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Task service config object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskServiceConfig {
    private int maxExecTaskSize = Integer.MAX_VALUE;
    private int maxTimedTaskSize = Integer.MAX_VALUE;
    private int workerSize = Runtime.getRuntime().availableProcessors();
    private long workerKeepAliveTime;//milliseconds
    private String poolImplementClassName = PoolTaskCenter.class.getName();

    public int getWorkerSize() {
        return workerSize;
    }

    public void setWorkerSize(int workerSize) {
        if (workerSize > 0) this.workerSize = workerSize;
    }

    public int getMaxExecTaskSize() {
        return maxExecTaskSize;
    }

    public void setMaxExecTaskSize(int maxExecTaskSize) {
        if (maxExecTaskSize > 0) {
            this.maxExecTaskSize = maxExecTaskSize;
            this.workerSize = Math.min(maxExecTaskSize, Runtime.getRuntime().availableProcessors());
        }
    }

    public int getMaxTimedTaskSize() {
        return maxTimedTaskSize;
    }

    public void setMaxTimedTaskSize(int maxTimedTaskSize) {
        if (maxTimedTaskSize > 0)
            this.maxTimedTaskSize = maxTimedTaskSize;
    }

    public long getWorkerKeepAliveTime() {
        return workerKeepAliveTime;
    }

    public void setWorkerKeepAliveTime(long workerKeepAliveTime) {
        if (workerKeepAliveTime > 0L) this.workerKeepAliveTime = workerKeepAliveTime;
    }

    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (isNotBlank(this.poolImplementClassName))
            this.poolImplementClassName = poolImplementClassName;
    }

    public TaskServiceConfig check() throws TaskServiceConfigException {
        TaskServiceConfig checkedConfig = new TaskServiceConfig();
        copyTo(checkedConfig);
        return checkedConfig;
    }

    void copyTo(TaskServiceConfig config) throws TaskServiceConfigException {
        String fieldName = "";
        try {
            for (Field field : TaskServiceConfig.class.getDeclaredFields()) {
                fieldName = field.getName();
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    field.set(config, field.get(this));
                }
            }
        } catch (Throwable e) {
            throw new TaskServiceConfigException("Failed to copy field[" + fieldName + "]", e);
        }
    }
}
