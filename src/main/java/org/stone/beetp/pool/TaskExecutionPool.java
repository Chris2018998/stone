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
import org.stone.beetp.pool.exception.PoolSubmitRejectedException;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolStaticCenter.*;

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
    private int maxQueueTaskSize;
    private int maxWorkerSize;
    private boolean workerInDaemon;
    private int poolFullPolicyCode;
    private TaskPoolMonitorVo monitorVo;
    private BeeTaskPoolInterceptor poolInterceptor;

    private AtomicInteger taskCount;
    private AtomicInteger workerCount;
    private ConcurrentLinkedQueue taskQueue;
    private ConcurrentLinkedQueue workerQueue;
    private ConcurrentLinkedQueue<Thread> poolTerminateWaitQueue;

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeTaskServiceConfig config) throws BeeTaskPoolException {
        //step1: config check
        if (config == null) throw new PoolInitializedException("Configuration can't be null");
        BeeTaskServiceConfig checkedConfig = config.check();

        //step2: task queue create
        this.taskCount = new AtomicInteger(0);
        this.workerCount = new AtomicInteger(0);
        this.taskQueue = new ConcurrentLinkedQueue();
        this.workerQueue = new ConcurrentLinkedQueue();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue();

        //step3: simple attribute set
        this.poolName = checkedConfig.getPoolName();
        this.maxQueueTaskSize = checkedConfig.getMaxQueueTaskSize();
        this.maxWorkerSize = checkedConfig.getMaxWorkerSize();
        this.workerInDaemon = checkedConfig.isWorkerInDaemon();
        this.poolFullPolicyCode = checkedConfig.getPoolFullPolicyCode();
        this.monitorVo = new TaskPoolMonitorVo();
        this.poolInterceptor = checkedConfig.getPoolInterceptor();
        this.poolState = POOL_READY;
    }

    //***************************************************************************************************************//
    //                2: task submit methods(2)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws BeeTaskPoolException {
        //1: check pool state
        if (task == null) throw new NullPointerException("Task can't be null");
        if (this.poolState != POOL_READY)
            throw new PoolSubmitRejectedException("Access forbidden,generic object pool was closed or in clearing");

        //2: check pool size full
        if (taskCount.get() == maxQueueTaskSize) {

        }

        //3: check pool size full
        TaskHandleImpl taskHandle = new TaskHandleImpl(task, this);
        taskQueue.offer(taskHandle);

        //4: wakeup a thread to process the task or create a new worker thread


        return taskHandle;
    }

    //***************************************************************************************************************//
    //                3: Pool terminate and clear(5)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return poolState == POOL_TERMINATED;
    }

    public boolean isTerminating() {
        return poolState == POOL_TERMINATING;
    }

    public List<BeeTask> terminate(boolean cancelRunningTask) throws BeeTaskPoolException {
        return null;
    }


    private void wakeupTerminationWaiters() {
        for (Thread thread : poolTerminateWaitQueue)
            LockSupport.unpark(thread);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (this.poolState == POOL_TERMINATED) return true;
        if (timeout < 0) throw new InvalidParameterException("Time out value must be greater than zero");
        if (unit == null) throw new InvalidParameterException("Time unit can't be null");

        Thread currentThread = Thread.currentThread();
        poolTerminateWaitQueue.offer(currentThread);

        long timeoutNano = unit.toNanos(timeout);
        boolean timed = timeoutNano > 0;
        long deadline = System.nanoTime() + timeoutNano;

        try {
            do {
                int poolStateCode = this.poolState;
                if (poolStateCode == POOL_TERMINATED) return true;

                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime > 0)
                        LockSupport.parkNanos(parkTime);
                    else
                        return this.poolState == POOL_TERMINATED;
                } else {
                    LockSupport.park();
                }
                if (currentThread.isInterrupted()) throw new InterruptedException();
            } while (true);
        } finally {
            poolTerminateWaitQueue.remove(currentThread);
        }
    }

    public void clear(boolean cancelRunningTask) throws BeeTaskPoolException {

    }

    //***************************************************************************************************************//
    //                4: Pool monitor(1)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        return monitorVo;
    }
}
