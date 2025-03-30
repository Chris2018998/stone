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
import org.stone.beetp.pool.PoolThreadFactory;
import org.stone.beetp.pool.exception.TaskServiceConfigException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.stone.tools.BeanUtil.createClassInstance;
import static org.stone.tools.BeanUtil.loadClass;
import static org.stone.tools.CommonUtil.isNotBlank;
import static org.stone.tools.CommonUtil.trimString;

/**
 * Task service config object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskServiceConfig {
    private int maxTaskSize = 1000;
    private int maxScheduleTaskSize = 1000;
    private int workerSize = Runtime.getRuntime().availableProcessors();
    private long workerKeepAliveTime;//milliseconds

    private TaskPoolThreadFactory threadFactory;
    private Class<TaskPoolThreadFactory> threadFactoryClass;
    private String threadFactoryClassName = PoolThreadFactory.class.getName();
    private String poolImplementClassName = PoolTaskCenter.class.getName();

    public int getWorkerSize() {
        return workerSize;
    }

    public void setWorkerSize(int workerSize) {
        if (workerSize > 0) this.workerSize = workerSize;
    }

    public int getMaxTaskSize() {
        return maxTaskSize;
    }

    public void setMaxTaskSize(int maxTaskSize) {
        if (maxTaskSize > 0) {
            this.maxTaskSize = maxTaskSize;
            this.workerSize = Math.min(maxTaskSize, Runtime.getRuntime().availableProcessors());
        }
    }

    public int getMaxScheduleTaskSize() {
        return maxScheduleTaskSize;
    }

    public void setMaxScheduleTaskSize(int maxScheduleTaskSize) {
        if (maxScheduleTaskSize > 0)
            this.maxScheduleTaskSize = maxScheduleTaskSize;
    }

    public long getWorkerKeepAliveTime() {
        return workerKeepAliveTime;
    }

    public void setWorkerKeepAliveTime(long workerKeepAliveTime) {
        if (workerKeepAliveTime > 0L) this.workerKeepAliveTime = workerKeepAliveTime;
    }


    public TaskPoolThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(TaskPoolThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public Class<TaskPoolThreadFactory> getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(Class<TaskPoolThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
    }

    public String getThreadFactoryClassName() {
        return threadFactoryClassName;
    }

    public void setThreadFactoryClassName(String threadFactoryClassName) {
        if (isNotBlank(threadFactoryClassName))
            this.threadFactoryClassName = trimString(threadFactoryClassName);
    }

    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (isNotBlank(poolImplementClassName))
            this.poolImplementClassName = trimString(poolImplementClassName);
    }

    public TaskServiceConfig check() throws TaskServiceConfigException {
        if (maxScheduleTaskSize > maxTaskSize)
            throw new TaskServiceConfigException("Max schedule task size can't be greater than max task size");
        TaskPoolThreadFactory threadFactory = createTaskPoolThreadFactory();

        TaskServiceConfig checkedConfig = new TaskServiceConfig();
        copyTo(checkedConfig);
        checkedConfig.threadFactory = threadFactory;
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

    private TaskPoolThreadFactory createTaskPoolThreadFactory() throws TaskServiceConfigException {
        if (threadFactory != null) return this.threadFactory;

        if (threadFactoryClass != null || isNotBlank(threadFactoryClassName)) {
            Class<?> factoryClass = null;
            try {
                factoryClass = threadFactoryClass != null ? threadFactoryClass : loadClass(threadFactoryClassName);
                return (TaskPoolThreadFactory) createClassInstance(factoryClass, TaskPoolThreadFactory.class, "thread factory");
            } catch (ClassNotFoundException e) {
                throw new TaskServiceConfigException("Not found thread factory class[" + threadFactoryClassName + "]", e);
            } catch (Throwable e) {
                throw new TaskServiceConfigException("Failed to create thread factory with class[" + factoryClass + "]", e);
            }
        }
        return null;
    }
}
