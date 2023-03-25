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
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beetp.BeeTaskServiceConfig.*;
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
    private long workerMaxAliveTime;
    private TaskRejectPolicy rejectPolicy;
    private TaskPoolMonitorVo monitorVo;
    private BeeTaskPoolInterceptor poolInterceptor;

    private AtomicInteger taskCount;
    private AtomicInteger workerCount;
    private ConcurrentLinkedQueue<TaskHandleImpl> taskQueue;
    private ConcurrentLinkedQueue<PoolWorkerThread> workerQueue;
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
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.workerQueue = new ConcurrentLinkedQueue<>();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        //step3: simple attribute set
        this.poolName = checkedConfig.getPoolName();
        this.maxQueueTaskSize = checkedConfig.getMaxQueueTaskSize();
        this.maxWorkerSize = checkedConfig.getMaxWorkerSize();
        this.workerInDaemon = checkedConfig.isWorkerInDaemon();
        this.workerMaxAliveTime = MILLISECONDS.toNanos(checkedConfig.getWorkerMaxAliveTime());
        switch (checkedConfig.getPoolFullPolicyCode()) {
            case Policy_Abort: {
                rejectPolicy = new TaskAbortPolicy();
                break;
            }
            case Policy_Discard: {
                rejectPolicy = new TaskDiscardPolicy();
                break;
            }
            case Policy_Remove_Oldest: {
                rejectPolicy = new TaskRemoveOldestPolicy();
                break;
            }
            case Policy_Caller_Runs: {
                rejectPolicy = new TaskCallerRunsPolicy();
                break;
            }
        }

        this.monitorVo = new TaskPoolMonitorVo();
        this.poolInterceptor = checkedConfig.getPoolInterceptor();
        this.poolState = POOL_READY;
    }

    //***************************************************************************************************************//
    //                2: task submit methods(2)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    void removeTask(BeeTaskHandle task) {
        taskQueue.remove(task);
    }

    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException, BeeTaskPoolException {
        //1: check pool state
        if (task == null) throw new TaskExecutionException("Task can't be null");
        if (this.poolState != POOL_READY)
            throw new PoolSubmitRejectedException("Access forbidden,generic object pool was closed or in clearing");

        //2: execute reject policy on task queue full
        boolean offerInd = true;
        if (taskCount.get() == maxQueueTaskSize) offerInd = rejectPolicy.rejectTask(task, this);

        //3: create task handle and offer it to queue by indicator
        TaskHandleImpl taskHandle = new TaskHandleImpl(task, offerInd ? TASK_NEW : TASK_CANCELLED, this);
        if (offerInd) {
            taskQueue.offer(taskHandle);
            //4: wakeup a worker to execute the task in async mode(@todo)
        }
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

    public List<BeeTask> terminate(boolean mayInterruptIfRunning) throws BeeTaskPoolException {
        wakeupTerminationWaiters();
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

    public void clear(boolean mayInterruptIfRunning) throws BeeTaskPoolException {

    }

    //***************************************************************************************************************//
    //                4: Pool monitor(1)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        return monitorVo;
    }

    //***************************************************************************************************************//
    //                5: Inner interfaces and classes (7)                                                            //                                                                                  //
    //***************************************************************************************************************//
    private interface TaskRejectPolicy {
        //true:rejected;false:continue;
        boolean rejectTask(BeeTask task, TaskExecutionPool pool) throws BeeTaskException, BeeTaskPoolException;
    }

    private static class PoolWorkerThread extends Thread {
        private static final AtomicInteger Index = new AtomicInteger(1);
        private final AtomicInteger state;
        private final TaskExecutionPool pool;

        public PoolWorkerThread(TaskExecutionPool pool, String name, boolean daemon) {
            this.pool = pool;
            this.setDaemon(daemon);
            this.setName(name + "-worker thread" + Index.getAndIncrement());
            this.state = new AtomicInteger(WORKER_IDLE);
        }

        void setState(int update) {
            state.set(update);
        }

        boolean compareAndSetState(int expect, int update) {
            return state.compareAndSet(expect, update);
        }

        public void run() {


        }
    }

    private static class TaskAbortPolicy implements TaskRejectPolicy {
        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) throws BeeTaskPoolException {
            throw new PoolSubmitRejectedException("");
        }
    }

    private static class TaskDiscardPolicy implements TaskRejectPolicy {
        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) {
            return true;
        }
    }

    private static class TaskRemoveOldestPolicy implements TaskRejectPolicy {
        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) {
            TaskHandleImpl oldTask = pool.taskQueue.poll();
            if (oldTask != null) {
                pool.taskCount.decrementAndGet();
                oldTask.compareAndSetState(TASK_NEW, TASK_CANCELLED);
            }
            return false;
        }
    }

    private static class TaskCallerRunsPolicy implements TaskRejectPolicy {
        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) throws BeeTaskException {
            try {
                task.call();
                return true;
            } catch (Throwable e) {
                throw new BeeTaskException(e);
            }
        }
    }
}
