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

import org.stone.beetp.*;
import org.stone.beetp.pool.exception.PoolInitializedException;
import org.stone.beetp.pool.exception.PoolSubmitRejectedException;
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.security.InvalidParameterException;
import java.util.LinkedList;
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

    private AtomicInteger taskCountInQueue;
    private AtomicInteger workerCountInQueue;
    private AtomicInteger runningTaskCount;
    private AtomicInteger completedTaskCount;
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
        this.taskCountInQueue = new AtomicInteger(0);
        this.workerCountInQueue = new AtomicInteger(0);
        this.runningTaskCount = new AtomicInteger(0);
        this.completedTaskCount = new AtomicInteger(0);
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.workerQueue = new ConcurrentLinkedQueue<>();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        //step3: simple attribute set
        this.poolName = checkedConfig.getPoolName();
        this.maxQueueTaskSize = checkedConfig.getMaxQueueTaskSize();
        this.maxWorkerSize = checkedConfig.getMaxWorkerSize();
        this.workerInDaemon = checkedConfig.isWorkerInDaemon();
        this.workerMaxAliveTime = MILLISECONDS.toNanos(checkedConfig.getWorkerMaxAliveTime());
        this.monitorVo = new TaskPoolMonitorVo();
        this.poolInterceptor = checkedConfig.getPoolInterceptor();
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
        this.poolState = POOL_READY;
    }

    //***************************************************************************************************************//
    //                2: task submit methods(2)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    void removeTask(TaskHandleImpl handle) {
        taskQueue.remove(handle);
    }

    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException, BeeTaskPoolException {
        //1: check pool state
        if (task == null) throw new TaskExecutionException("Task can't be null");
        if (this.poolState != POOL_READY)
            throw new PoolSubmitRejectedException("Access forbidden,generic object pool was closed or in clearing");

        //2: execute reject policy on task queue full
        boolean offerInd = true;
        if (taskCountInQueue.get() == maxQueueTaskSize) offerInd = rejectPolicy.rejectTask(task, this);

        //3: create task handle and offer it to queue by indicator
        TaskHandleImpl taskHandle = new TaskHandleImpl(task, offerInd ? TASK_NEW : TASK_CANCELLED, this);
        if (offerInd) {
            taskQueue.offer(taskHandle);
            wakeUpOneWorker();
        }
        return taskHandle;
    }

    private void wakeUpOneWorker() {
        for (PoolWorkerThread workerThread : workerQueue) {
            if (workerThread.compareAndSetState(WORKER_IDLE, WORKER_RUNNING)) {
                LockSupport.unpark(workerThread);
                return;
            }
        }

        do {
            int currentCount = this.workerCountInQueue.get();
            if (currentCount >= this.maxWorkerSize) return;
            if (workerCountInQueue.compareAndSet(currentCount, currentCount + 1)) {
                PoolWorkerThread worker = new PoolWorkerThread(this);
                workerQueue.offer(worker);
                worker.start();
                return;
            }
        } while (true);
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
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_TERMINATING)) {
            PoolWorkerThread worker;
            while ((worker = workerQueue.poll()) != null) {
                worker.setState(WORKER_TERMINATED);
                worker.interrupt();
            }

            TaskHandleImpl taskHandle;
            List<BeeTask> queueTaskList = new LinkedList<>();
            while ((taskHandle = taskQueue.poll()) != null) {
                queueTaskList.add(taskHandle.getTask());
                taskHandle.setState(TASK_CANCELLED);
                taskHandle.wakeupWaiters();
            }

            this.poolState = POOL_TERMINATED;
            this.wakeupTerminationWaiters();
            return queueTaskList;
        } else {
            throw new BeeTaskPoolException("Termination forbidden,pool has been in terminating or terminated");
        }
    }

    public boolean clear(boolean mayInterruptIfRunning) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            TaskHandleImpl taskHandle;
            while ((taskHandle = taskQueue.poll()) != null) {
                taskHandle.setState(TASK_CANCELLED);
                taskHandle.wakeupWaiters();
            }

            if (mayInterruptIfRunning) {
                PoolWorkerThread worker;
                while ((worker = workerQueue.poll()) != null) {
                    worker.setState(WORKER_TERMINATED);
                    worker.interrupt();
                }
            }
            this.poolState = POOL_READY;
            return true;
        } else {
            return false;
        }
    }

    private void wakeupTerminationWaiters() {
        for (Thread thread : poolTerminateWaitQueue)
            LockSupport.unpark(thread);
        poolTerminateWaitQueue.clear();
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

                if (Thread.interrupted()) throw new InterruptedException();
            } while (true);
        } finally {
            poolTerminateWaitQueue.remove(currentThread);
        }
    }

    //execute task
    private void executeTask(TaskHandleImpl handle) {
        try {
            workerCountInQueue.decrementAndGet();
            if (handle.compareAndSetState(TASK_NEW, TASK_RUNNING)) {
                runningTaskCount.incrementAndGet();
                BeeTask task = handle.getTask();
                //1: execute pool interceptor
                if (poolInterceptor != null) {
                    try {
                        poolInterceptor.beforeCall(task);
                    } catch (Throwable e) {
                        //do nothing
                    }
                }
                //2: execute task aspect
                BeeTaskAspect aspect = task.getAspect();
                if (aspect != null) {
                    try {
                        aspect.beforeCall();
                    } catch (Throwable e) {
                        //do nothing
                    }
                }
                //3: execute task
                try {
                    Object result = task.call();
                    handle.setResult(result);
                    if (poolInterceptor != null) {
                        try {
                            poolInterceptor.afterCall(task, result);
                        } catch (Throwable e) {
                            //do nothing
                        }
                    }
                    if (aspect != null) {
                        try {
                            aspect.afterCall(result);
                        } catch (Throwable e) {
                            //do nothing
                        }
                    }
                } catch (Throwable e) {
                    handle.setException(new TaskExecutionException(e));
                    if (poolInterceptor != null) {
                        try {
                            poolInterceptor.AfterThrowing(task, e);
                        } catch (Throwable ee) {
                            //do nothing
                        }
                    }
                    if (aspect != null) {
                        try {
                            aspect.AfterThrowing(e);
                        } catch (Throwable ee) {
                            //do nothing
                        }
                    }
                }
            }
        } finally {
            runningTaskCount.decrementAndGet();
            completedTaskCount.incrementAndGet();
        }
    }

    //***************************************************************************************************************//
    //                4: Pool monitor(1)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setWorkerCount(workerCountInQueue.get());
        monitorVo.setQueueTaskCount(taskCountInQueue.get());
        monitorVo.setRunningTaskCount(runningTaskCount.get());
        monitorVo.setCompletedTaskCount(completedTaskCount.get());
        return monitorVo;
    }

    //***************************************************************************************************************//
    //                5: Inner interfaces and classes (7)                                                            //                                                                                  //
    //***************************************************************************************************************//
    private interface TaskRejectPolicy {
        //true:rejected;false:continue;
        boolean rejectTask(BeeTask task, TaskExecutionPool pool) throws BeeTaskException, BeeTaskPoolException;
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
                pool.taskCountInQueue.decrementAndGet();
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

    private static class PoolWorkerThread extends Thread {
        private static final AtomicInteger Index = new AtomicInteger(1);
        private final AtomicInteger state;
        private final TaskExecutionPool pool;
        private final boolean keepaliveTimed;
        private final long workerKeepAliveTime;
        private final ConcurrentLinkedQueue<TaskHandleImpl> taskQueue;

        PoolWorkerThread(TaskExecutionPool pool) {
            this.pool = pool;
            this.setDaemon(pool.workerInDaemon);
            this.taskQueue = pool.taskQueue;
            this.workerKeepAliveTime = pool.workerMaxAliveTime;
            this.keepaliveTimed = workerKeepAliveTime > 0;
            this.setName(pool.poolName + "-worker thread" + Index.getAndIncrement());
            this.state = new AtomicInteger(WORKER_RUNNING);
        }

        void setState(int update) {
            state.set(update);
        }

        boolean compareAndSetState(int expect, int update) {
            return state.compareAndSet(expect, update);
        }

        public void run() {
            do {
                int stateCode = state.get();
                if (stateCode == WORKER_TERMINATED) {
                    pool.workerCountInQueue.decrementAndGet();
                    pool.workerQueue.remove(this);
                    break;
                }

                //poll task from queue
                TaskHandleImpl task = taskQueue.poll();
                if (task != null) {
                    pool.executeTask(task);
                } else if (compareAndSetState(WORKER_RUNNING, WORKER_IDLE)) {
                    if (keepaliveTimed)
                        LockSupport.parkNanos(workerKeepAliveTime);
                    else
                        LockSupport.park();

                    //keep alive timeout,then try to exit
                    compareAndSetState(WORKER_IDLE, WORKER_TERMINATED);
                }
            } while (true);
        }
    }
}
