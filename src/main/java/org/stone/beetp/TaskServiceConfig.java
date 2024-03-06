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

import org.stone.beetp.pool.TaskExecutionPool;
import org.stone.beetp.pool.exception.TaskServiceConfigException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.tools.CommonUtil.isBlank;

/**
 * Task service config
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskServiceConfig {
    //index for generating default pool name,atomic value starts with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);

    //if this value is null or empty,a generated pool name set to this field
    private String poolName;
    ///max reachable size of task instance by per key(once tasks + scheduled tasks)
    private int maxTaskSize = 100;
    //worker creation size while pool initialization
    private int initWorkerSize;
    //max worker siz in pool
    private int maxWorkerSize = Runtime.getRuntime().availableProcessors();
    //daemon ind of worker thread
    private boolean workInDaemon;
    //idle timeout of worker thread(zero value means not timeout)
    private long workerKeepAliveTime;
    //pool implementation class name
    private String poolImplementClassName = TaskExecutionPool.class.getName();

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

    public int getInitWorkerSize() {
        return initWorkerSize;
    }

    public void setInitWorkerSize(int initWorkerSize) {
        if (initWorkerSize > 0) this.initWorkerSize = initWorkerSize;
    }

    public int getMaxWorkerSize() {
        return maxWorkerSize;
    }

    public void setMaxWorkerSize(int maxWorkerSize) {
        if (maxWorkerSize > 0) this.maxWorkerSize = maxWorkerSize;
    }

    public boolean isWorkInDaemon() {
        return workInDaemon;
    }

    public void setWorkInDaemon(boolean workInDaemon) {
        this.workInDaemon = workInDaemon;
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
        if (!isBlank(this.poolImplementClassName))
            this.poolImplementClassName = poolImplementClassName;
    }

    public TaskServiceConfig check() throws TaskServiceConfigException {
        if (maxTaskSize <= 0)
            throw new TaskServiceConfigException("maxTaskSize must be greater than zero");
        if (initWorkerSize < 0)
            throw new TaskServiceConfigException("initWorkerSize must be not less than zero");
        if (maxWorkerSize <= 0)
            throw new TaskServiceConfigException("maxWorkerSize must be greater than zero");
        if (maxWorkerSize < initWorkerSize)
            throw new TaskServiceConfigException("maxWorkerSize must be not less than initWorkerSize");
        if (workerKeepAliveTime < 0L)
            throw new TaskServiceConfigException("workerKeepAliveTime must be greater than zero");

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
