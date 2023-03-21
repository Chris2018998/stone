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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beetp.*;
import org.stone.beetp.pool.exception.PoolInitializedException;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Task Pool Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskExecutionPool implements BeeTaskPool {
    private static final Logger Log = LoggerFactory.getLogger(TaskExecutionPool.class);
    private static final AtomicIntegerFieldUpdater<TaskExecutionPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(TaskExecutionPool.class, "poolState");

    private String poolName;
    private volatile int poolState;
    private TaskPoolMonitorVo monitorVo;
    private BeeTaskPoolInterceptor poolInterceptor;

    private AtomicInteger taskCount;
    private AtomicInteger workerCount;
    private ConcurrentLinkedQueue taskQueue;
    private ConcurrentLinkedQueue workerQueue;

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeTaskServiceConfig config) throws Exception {
        //step1: config check
        if (config == null) throw new PoolInitializedException("Configuration can't be null");
        BeeTaskServiceConfig checkedConfig = config.check();

        //step2: simple attribute set
        this.poolName = checkedConfig.getPoolName();
        this.monitorVo = new TaskPoolMonitorVo();
        this.poolInterceptor = checkedConfig.getPoolInterceptor();

        //step3: task queue create
        this.taskCount = new AtomicInteger(0);
        this.workerCount = new AtomicInteger(0);
        this.taskQueue = new ConcurrentLinkedQueue();
        this.workerQueue = new ConcurrentLinkedQueue();
    }

    //***************************************************************************************************************//
    //                2: task submit methods(2)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws Exception {
        return null;
        //@todo
    }

    public List<BeeTaskHandle> submit(List<BeeTask> taskList) throws Exception {
        return null;
        //@todo
    }

    //***************************************************************************************************************//
    //                3: Pool terminate and clear(5)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return true;
        //@todo
    }

    public boolean isTerminating() {
        return true;
        //@todo
    }

    public List<BeeTask> terminate(boolean cancelRunningTask) throws BeeTaskPoolException {
        return null;
        //@todo
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
        //@todo
    }

    public void clear(boolean cancelRunningTask) throws BeeTaskPoolException {

    }

    //***************************************************************************************************************//
    //                4: Pool monitor(1)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        return null;
        //@todo
    }
}
