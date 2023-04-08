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

import static org.stone.util.CommonUtil.isBlank;

/**
 * Task service config
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeTaskServiceConfig {
    //action: throw PoolSubmitRejectedException when pool full
    public static final int Policy_Abort = 1;
    //action: do nothing when pool full
    public static final int Policy_Discard = 2;
    //action: remove oldest task and offer to pool
    public static final int Policy_Remove_Oldest = 3;
    //action: execute task by caller when pool full
    public static final int Policy_Caller_Runs = 4;
    //index on pool name generation
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);

    private String poolName;
    private int queueMaxSize;
    private int initWorkerSize;
    private int maxWorkerSize;
    private boolean workInDaemon;
    private long workerKeepAliveTime;
    private int queueFullPolicyCode = Policy_Abort;

    private String poolInterceptorClassName;
    private BeeTaskPoolInterceptor poolInterceptor;
    private String poolImplementClassName = TaskExecutionPool.class.getName();

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getQueueMaxSize() {
        return queueMaxSize;
    }

    public void setQueueMaxSize(int queueMaxSize) {
        this.queueMaxSize = queueMaxSize;
    }

    public int getInitWorkerSize() {
        return initWorkerSize;
    }

    public void setInitWorkerSize(int initWorkerSize) {
        this.initWorkerSize = initWorkerSize;
    }

    public int getMaxWorkerSize() {
        return maxWorkerSize;
    }

    public void setMaxWorkerSize(int maxWorkerSize) {
        this.maxWorkerSize = maxWorkerSize;
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
        this.workerKeepAliveTime = workerKeepAliveTime;
    }

    public int getQueueFullPolicyCode() {
        return queueFullPolicyCode;
    }

    public void setQueueFullPolicyCode(int queueFullPolicyCode) {
        if (queueFullPolicyCode >= Policy_Abort && queueFullPolicyCode <= Policy_Caller_Runs)
            this.queueFullPolicyCode = queueFullPolicyCode;
    }

    public String getPoolInterceptorClassName() {
        return poolInterceptorClassName;
    }

    public void setPoolInterceptorClassName(String poolInterceptorClassName) {
        this.poolInterceptorClassName = poolInterceptorClassName;
    }

    public BeeTaskPoolInterceptor getPoolInterceptor() {
        return poolInterceptor;
    }

    public void setPoolInterceptor(BeeTaskPoolInterceptor poolInterceptor) {
        this.poolInterceptor = poolInterceptor;
    }

    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (!isBlank(this.poolImplementClassName))
            this.poolImplementClassName = poolImplementClassName;
    }

    public BeeTaskServiceConfig check() {
        if (queueMaxSize <= 0)
            throw new BeeTaskServiceConfigException("queueMaxSize must be greater than zero");
        if (initWorkerSize < 0)
            throw new BeeTaskServiceConfigException("initWorkerSize must be not less than zero");
        if (maxWorkerSize <= 0)
            throw new BeeTaskServiceConfigException("maxWorkerSize must be greater than zero");
        if (maxWorkerSize < initWorkerSize)
            throw new BeeTaskServiceConfigException("maxWorkerSize must be not less than initWorkerSize");
        if (workerKeepAliveTime < 0)
            throw new BeeTaskServiceConfigException("workerKeepAliveTime must be greater than zero");

        //1: check pool full policy code
        if (queueFullPolicyCode < Policy_Abort || queueFullPolicyCode > Policy_Caller_Runs)
            throw new BeeTaskServiceConfigException("invalid queueFullPolicyCode");

        //2: try to create Interceptor
        BeeTaskPoolInterceptor tempInterceptor = this.poolInterceptor;
        if (tempInterceptor == null && !isBlank(this.poolInterceptorClassName)) {
            try {
                Class interceptorClassClass = Class.forName(this.poolInterceptorClassName);
                if (!BeeTaskPoolInterceptor.class.isAssignableFrom(interceptorClassClass))
                    throw new BeeTaskServiceConfigException("Not found Interceptor class:" + this.poolInterceptorClassName);

                tempInterceptor = (BeeTaskPoolInterceptor) interceptorClassClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new BeeTaskServiceConfigException("Not found object factory class:" + this.poolInterceptorClassName);
            } catch (Throwable e) {
                throw new BeeTaskServiceConfigException("Failed to create object factory by class:" + poolInterceptorClassName, e);
            }
        }

        //3:create new config and copy field value from current
        BeeTaskServiceConfig checkedConfig = new BeeTaskServiceConfig();
        copyTo(checkedConfig);

        //4:set pool name and interceptor
        if (tempInterceptor != null) checkedConfig.poolInterceptor = tempInterceptor;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "TaskPool-" + PoolNameIndex.getAndIncrement();

        if (checkedConfig.queueMaxSize == 0) checkedConfig.queueMaxSize = 100;
        if (checkedConfig.maxWorkerSize == 0) checkedConfig.maxWorkerSize = Runtime.getRuntime().availableProcessors();
        return checkedConfig;
    }

    void copyTo(BeeTaskServiceConfig config) {
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
