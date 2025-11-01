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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;

import static org.stone.tools.BeanUtil.createClassInstance;
import static org.stone.tools.BeanUtil.loadClass;
import static org.stone.tools.CommonUtil.*;

/**
 * Task service config object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskServiceConfig {
    //size of once tasks
    private int maxOnceTaskSize = 1000;
    private int onceWorkerCount = Runtime.getRuntime().availableProcessors();
    private long onceWorkerKeepAliveTime = TimeUnit.SECONDS.toMillis(15L);//milliseconds

    //size of timer tasks
    private int maxTimerTaskSize;
    private int timerWorkerCount;
    private long timerWorkerKeepAliveTime;//milliseconds

    //size of joined tasks
    private int maxJoinTaskSize;
    private int joinWorkerCount;
    private long joinWorkerKeepAliveTime;//milliseconds

    //thread factory
    private TaskPoolThreadFactory threadFactory;
    private Class<TaskPoolThreadFactory> threadFactoryClass;
    private String threadFactoryClassName = PoolThreadFactory.class.getName();

    //pool implementation class name
    private String poolImplementClassName = PoolTaskCenter.class.getName();


    //***************************************************************************************************************//
    //                                    Constructors (4)                                                           //
    //***************************************************************************************************************//
    public TaskServiceConfig() {
    }

    public TaskServiceConfig(int maxOnceTaskSize, int onceWorkerCount, long onceWorkerKeepAliveTime) {
        this(maxOnceTaskSize, onceWorkerCount, onceWorkerKeepAliveTime,
                0, 0, 0L,
                0, 0, 0L);
    }

    public TaskServiceConfig(int maxOnceTaskSize, int onceWorkerCount, long onceWorkerKeepAliveTime,
                             int maxTimerTaskSize, int timerWorkerCount, long timerWorkerKeepAliveTime) {

        this(maxOnceTaskSize, onceWorkerCount, onceWorkerKeepAliveTime,
                maxTimerTaskSize, timerWorkerCount, timerWorkerKeepAliveTime,
                0, 0, 0L);
    }

    public TaskServiceConfig(int maxOnceTaskSize, int onceWorkerCount, long onceWorkerKeepAliveTime,
                             int maxTimerTaskSize, int timerWorkerCount, long timerWorkerKeepAliveTime,
                             int maxJoinTaskSize, int joinWorkerCount, long joinWorkerKeepAliveTime) {

        this.setMaxOnceTaskSize(maxOnceTaskSize);
        this.setOnceWorkerCount(onceWorkerCount);
        this.setOnceWorkerKeepAliveTime(onceWorkerKeepAliveTime);

        this.setMaxTimerTaskSize(maxTimerTaskSize);
        this.setTimerWorkerCount(timerWorkerCount);
        this.setTimerWorkerKeepAliveTime(timerWorkerKeepAliveTime);

        this.setMaxJoinTaskSize(maxJoinTaskSize);
        this.setJoinWorkerCount(joinWorkerCount);
        this.setJoinWorkerKeepAliveTime(joinWorkerKeepAliveTime);
    }

    //***************************************************************************************************************//
    //                                    Once task (6)                                                              //
    //***************************************************************************************************************//
    public int getMaxOnceTaskSize() {
        return maxOnceTaskSize;
    }

    public void setMaxOnceTaskSize(int maxOnceTaskSize) {
        if (maxOnceTaskSize >= 0) {
            this.maxOnceTaskSize = maxOnceTaskSize;
            this.onceWorkerCount = Math.min(maxOnceTaskSize, Runtime.getRuntime().availableProcessors());
        } else {
            throw new TaskServiceConfigException("The given value of item 'max-once-task-size' cannot be less than zero");
        }
    }

    public int getOnceWorkerCount() {
        return onceWorkerCount;
    }

    public void setOnceWorkerCount(int onceWorkerCount) {
        if (onceWorkerCount > 0) {
            this.onceWorkerCount = onceWorkerCount;
        } else {
            throw new TaskServiceConfigException("The given value of item 'once-worker-count' cannot be less than zero");
        }
    }

    public long getOnceWorkerKeepAliveTime() {
        return onceWorkerKeepAliveTime;
    }

    public void setOnceWorkerKeepAliveTime(long onceWorkerKeepAliveTime) {
        if (onceWorkerKeepAliveTime > 0L) {
            this.onceWorkerKeepAliveTime = onceWorkerKeepAliveTime;
        } else {
            throw new TaskServiceConfigException("The given value of item 'once-worker-keep-alive-time' cannot be less than zero");
        }
    }

    //***************************************************************************************************************//
    //                                    Timer task (6)                                                              //
    //***************************************************************************************************************//
    public int getMaxTimerTaskSize() {
        return maxTimerTaskSize;
    }

    public void setMaxTimerTaskSize(int maxTimerTaskSize) {
        if (maxTimerTaskSize >= 0) {
            this.maxTimerTaskSize = maxTimerTaskSize;
            if (this.timerWorkerCount == 0)
                this.timerWorkerCount = Math.min(maxTimerTaskSize, Runtime.getRuntime().availableProcessors());
            if (this.timerWorkerKeepAliveTime == 0) this.timerWorkerKeepAliveTime = TimeUnit.SECONDS.toMillis(15L);
        } else {
            throw new TaskServiceConfigException("The given value of item 'max-once-task-size' cannot be less than zero");
        }
    }


    public int getTimerWorkerCount() {
        return timerWorkerCount;
    }

    public void setTimerWorkerCount(int timerWorkerCount) {
        if (timerWorkerCount > 0) {
            this.timerWorkerCount = timerWorkerCount;
        } else {
            throw new TaskServiceConfigException("The given value of item 'timer-worker-count' cannot be less than zero");
        }
    }

    public long getTimerWorkerKeepAliveTime() {
        return timerWorkerKeepAliveTime;
    }

    public void setTimerWorkerKeepAliveTime(long timerWorkerKeepAliveTime) {
        if (timerWorkerKeepAliveTime > 0L) {
            this.timerWorkerKeepAliveTime = timerWorkerKeepAliveTime;
        } else {
            throw new TaskServiceConfigException("The given value of item 'timer-worker-keep-alive-time' cannot be less than zero");
        }
    }

    //***************************************************************************************************************//
    //                                    Join task (6)                                                              //
    //***************************************************************************************************************//
    public int getMaxJoinTaskSize() {
        return maxJoinTaskSize;
    }

    public void setMaxJoinTaskSize(int maxJoinTaskSize) {
        if (maxJoinTaskSize >= 0) {
            this.maxJoinTaskSize = maxJoinTaskSize;
            if (this.joinWorkerCount == 0)
                this.joinWorkerCount = Math.min(maxJoinTaskSize, Runtime.getRuntime().availableProcessors());
            if (this.joinWorkerKeepAliveTime == 0) this.timerWorkerKeepAliveTime = TimeUnit.SECONDS.toMillis(15L);

        } else {
            throw new TaskServiceConfigException("The given value of item 'max-join-task-size' cannot be less than zero");
        }
    }

    public int getJoinWorkerCount() {
        return joinWorkerCount;
    }

    public void setJoinWorkerCount(int joinWorkerCount) {
        if (joinWorkerCount > 0) {
            this.joinWorkerCount = joinWorkerCount;
        } else {
            throw new TaskServiceConfigException("The given value of item 'join-worker-count' cannot be less than zero");
        }
    }

    public long getJoinWorkerKeepAliveTime() {
        return joinWorkerKeepAliveTime;
    }

    public void setJoinWorkerKeepAliveTime(long joinWorkerKeepAliveTime) {
        if (joinWorkerKeepAliveTime > 0L) {
            this.joinWorkerKeepAliveTime = joinWorkerKeepAliveTime;
        } else {
            throw new TaskServiceConfigException("The given value of item 'join-worker-keep-alive-time' cannot be less than zero");
        }
    }

    //***************************************************************************************************************//
    //                                    Thread factory (6)                                                         //
    //***************************************************************************************************************//
    public TaskPoolThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(TaskPoolThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new TaskServiceConfigException("The given value of item 'thread-factory' cannot be null");
        this.threadFactory = threadFactory;
    }

    public Class<TaskPoolThreadFactory> getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(Class<TaskPoolThreadFactory> threadFactoryClass) {
        if (threadFactoryClass == null)
            throw new TaskServiceConfigException("The given value of item 'thread-factory-class' cannot be null");
        this.threadFactoryClass = threadFactoryClass;
    }

    public String getThreadFactoryClassName() {
        return threadFactoryClassName;
    }

    public void setThreadFactoryClassName(String threadFactoryClassName) {
        if (isBlank(threadFactoryClassName))
            throw new TaskServiceConfigException("The given value of item 'thread-factory-class name' cannot be null and blank");
        this.threadFactoryClassName = trimString(threadFactoryClassName);
    }

    //***************************************************************************************************************//
    //                                    pool class (6)                                                         //
    //***************************************************************************************************************/
    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (isBlank(poolImplementClassName))
            throw new TaskServiceConfigException("The given value of item 'pool-implement-class-name' cannot be null and blank");
        this.poolImplementClassName = poolImplementClassName;
    }

    public TaskServiceConfig check() throws TaskServiceConfigException {
        if (maxTimerTaskSize > maxOnceTaskSize)
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
