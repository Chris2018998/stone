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

import org.stone.beetp.pool.TaskExecutionPool;

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
public class BeeTaskServiceConfig {
    //index on pool name generation
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);

    //if not set,then generate with<code>BeeTaskServiceConfig.PoolNameIndex</code>
    private String poolName;
    //max size of tasks in pool(once count + scheduled count)
    private int taskMaxSize = 100;
    //worker creation size on pool initialization
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

    public int getTaskMaxSize() {
        return taskMaxSize;
    }

    public void setTaskMaxSize(int taskMaxSize) {
        if (taskMaxSize > 0) this.taskMaxSize = taskMaxSize;
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
        if (workerKeepAliveTime > 0) this.workerKeepAliveTime = workerKeepAliveTime;
    }

    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (!isBlank(this.poolImplementClassName))
            this.poolImplementClassName = poolImplementClassName;
    }

    public BeeTaskServiceConfig check() throws BeeTaskServiceConfigException {
        if (taskMaxSize <= 0)
            throw new BeeTaskServiceConfigException("taskMaxSize must be greater than zero");
        if (initWorkerSize < 0)
            throw new BeeTaskServiceConfigException("initWorkerSize must be not less than zero");
        if (maxWorkerSize <= 0)
            throw new BeeTaskServiceConfigException("maxWorkerSize must be greater than zero");
        if (maxWorkerSize < initWorkerSize)
            throw new BeeTaskServiceConfigException("maxWorkerSize must be not less than initWorkerSize");
        if (workerKeepAliveTime < 0)
            throw new BeeTaskServiceConfigException("workerKeepAliveTime must be greater than zero");

        //2:create new config and copy field value from current
        BeeTaskServiceConfig checkedConfig = new BeeTaskServiceConfig();
        copyTo(checkedConfig);

        //3:set pool name
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "TaskPool-" + PoolNameIndex.getAndIncrement();
        return checkedConfig;
    }

    void copyTo(BeeTaskServiceConfig config) throws BeeTaskServiceConfigException {
        String fieldName = "";
        try {
            for (Field field : BeeTaskServiceConfig.class.getDeclaredFields()) {
                fieldName = field.getName();
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    Object fieldValue = field.get(this);
                    field.set(config, fieldValue);
                }
            }
        } catch (Throwable e) {
            throw new BeeTaskServiceConfigException("Failed to copy field[" + fieldName + "]", e);
        }
    }
}
