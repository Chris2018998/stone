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

import org.stone.beetp.pool.TaskPoolImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.util.CommonUtil.isBlank;

/**
 * Task manager config
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeTaskServiceConfig {
    //action: throw TaskRejectedException when pool full
    public static final int Policy_Abort = 1;
    //action: do nothing when pool full
    public static final int Policy_Discard = 2;
    //action: remove oldest task and offer to pool
    public static final int Policy_Remove_Oldest = 3;
    //action: execute task by caller when pool full
    public static final int Policy_Caller_Runs = 4;
    //index on pool name generation
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);

    //pool name,if not set,auto generated by<code>BeeTaskServiceConfig.PoolNameIndex</code>
    private String poolName;

    private int maxQueueTaskSize;

    private int poolFullPolicyCode = Policy_Abort;

    private int maxWorkerSize;

    private boolean workerInDaemon;

    private String interceptorClassName;

    private BeeTaskInterceptor interceptor;

    //pool implementation class name
    private String poolImplementClassName = TaskPoolImpl.class.getName();


    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getMaxQueueTaskSize() {
        return maxQueueTaskSize;
    }

    public void setMaxQueueTaskSize(int maxQueueTaskSize) {
        this.maxQueueTaskSize = maxQueueTaskSize;
    }

    public int getPoolFullPolicyCode() {
        return poolFullPolicyCode;
    }

    public void setPoolFullPolicyCode(int poolFullPolicyCode) {
        if (poolFullPolicyCode >= Policy_Abort && poolFullPolicyCode <= Policy_Caller_Runs)
            this.poolFullPolicyCode = poolFullPolicyCode;
    }

    public int getMaxWorkerSize() {
        return maxWorkerSize;
    }

    public void setMaxWorkerSize(int maxWorkerSize) {
        this.maxWorkerSize = maxWorkerSize;
    }

    public boolean isWorkerInDaemon() {
        return workerInDaemon;
    }

    public void setWorkerInDaemon(boolean workerInDaemon) {
        this.workerInDaemon = workerInDaemon;
    }

    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    public void setInterceptorClassName(String interceptorClassName) {
        this.interceptorClassName = interceptorClassName;
    }

    public BeeTaskInterceptor getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(BeeTaskInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public String getPoolImplementClassName() {
        return poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (!isBlank(this.poolImplementClassName))
            this.poolImplementClassName = poolImplementClassName;
    }

    public BeeTaskServiceConfig check() {
        if (maxQueueTaskSize <= 0)
            throw new BeeTaskServiceConfigException("maxQueueTaskSize must be greater than zero");
        if (maxWorkerSize <= 0)
            throw new BeeTaskServiceConfigException("maxWorkerSize must be greater than zero");

        //1: check pool full policy code
        if (poolFullPolicyCode < Policy_Abort || poolFullPolicyCode > Policy_Caller_Runs)
            throw new BeeTaskServiceConfigException("invalid poolFullPolicyCode");

        //2: try to create Interceptor
        BeeTaskInterceptor tempInterceptor = this.interceptor;
        if (tempInterceptor == null && !isBlank(this.interceptorClassName)) {
            try {
                Class interceptorClassClass = Class.forName(this.interceptorClassName);
                if (!BeeTaskInterceptor.class.isAssignableFrom(interceptorClassClass))
                    throw new BeeTaskServiceConfigException("Not found Interceptor class:" + this.interceptorClassName);

                tempInterceptor = (BeeTaskInterceptor) interceptorClassClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new BeeTaskServiceConfigException("Not found object factory class:" + this.interceptorClassName);
            } catch (Throwable e) {
                throw new BeeTaskServiceConfigException("Failed to create object factory by class:" + interceptorClassName, e);
            }
        }

        //3:create new config and copy field value from current
        BeeTaskServiceConfig checkedConfig = new BeeTaskServiceConfig();
        copyTo(checkedConfig);

        //4:set pool name and interceptor
        if (tempInterceptor != null) checkedConfig.interceptor = tempInterceptor;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "TaskPool-" + PoolNameIndex.getAndIncrement();
        return checkedConfig;
    }

    void copyTo(BeeTaskServiceConfig config) {
        String fieldName = "";
        try {
            for (Field field : BeeTaskServiceConfig.class.getDeclaredFields()) {
                fieldName = field.getName();
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) && !"connectProperties".equals(fieldName)) {
                    Object fieldValue = field.get(this);
                    field.set(config, fieldValue);
                }
            }
        } catch (Throwable e) {
            throw new BeeTaskServiceConfigException("Failed to copy field[" + fieldName + "]", e);
        }
    }
}