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
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Task service config object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskServiceConfig {
    //an int sequence for pool names generation,its value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);

    //pool name,if not set,a generation name will be assigned to it
    private String poolName;
    //maximum of tasks,default is Integer.MAX_VALUE
    private int maxTaskSize = Integer.MAX_VALUE;

    //maximum of workers in pool,default is core size of cup
    private int workerSize = Runtime.getRuntime().availableProcessors();
    //a threshold task size to wake up all workers,its value not greater than max size of tasks,default=worker size * 2
    private int fullWorkTaskSize = workerSize * 2;

    //milliseconds:max idle time that no tasks to be processed
    private long workerKeepAliveTime;
    //class name of task pool implementation
    private String poolImplementClassName = PoolTaskCenter.class.getName();

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getMaxTaskSize() {
        return maxTaskSize;
    }

    public void setMaxTaskSize(int maxTaskSize) {
        if (maxTaskSize > 0) this.maxTaskSize = maxTaskSize;
    }

    public int getWorkerSize() {
        return workerSize;
    }

    public void setWorkerSize(int workerSize) {
        if (workerSize > 0) this.workerSize = workerSize;
    }

    public int getFullWorkTaskSize() {
        return fullWorkTaskSize;
    }

    public void setFullWorkTaskSize(int fullWorkTaskSize) {
        if (fullWorkTaskSize > 0) this.fullWorkTaskSize = fullWorkTaskSize;
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
        if (workerSize > 4 * Runtime.getRuntime().availableProcessors())
            throw new TaskServiceConfigException("worker-size can't be greater than 4 times of cpu core size");
        if (fullWorkTaskSize > maxTaskSize)
            throw new TaskServiceConfigException("full-work-task-size can't be greater than max task size");

        //2:create new config and copy field value from current
        TaskServiceConfig checkedConfig = new TaskServiceConfig();
        copyTo(checkedConfig);

        //3:set pool name
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "TaskPool-" + PoolNameIndex.getAndIncrement();
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
