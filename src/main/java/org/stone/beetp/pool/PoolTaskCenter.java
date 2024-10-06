/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

import org.stone.beetp.*;
import org.stone.beetp.pool.exception.*;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beetp.pool.PoolConstants.*;
import static org.stone.tools.CommonUtil.maxTimedSpins;
import static org.stone.tools.CommonUtil.maxUntimedSpins;

/**
 * Task Pool Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class PoolTaskCenter implements TaskPool {
    private static final AtomicIntegerFieldUpdater<PoolTaskCenter> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(PoolTaskCenter.class, "poolState");
    private static final AtomicIntegerFieldUpdater<PoolTaskCenter> TaskCountUpd = IntegerFieldUpdaterImpl.newUpdater(PoolTaskCenter.class, "submitTaskCount");
    private static final AtomicIntegerFieldUpdater<PoolTaskCenter> ScheduledTaskCountUpd = IntegerFieldUpdaterImpl.newUpdater(PoolTaskCenter.class, "scheduledTaskCount");
    private volatile int poolState;
    private PoolMonitorVo monitorVo;

    private int maxTaskSize;
    private int maxScheduleTaskSize;
    private volatile int taskCount;
    private volatile int scheduledTaskCount;

    private int workerSize;
    private int maxNoOfWorkers;
    private TaskExecutionWorker[] workers;
    private TaskExecutionNotifier notifier;
    private ConcurrentLinkedQueue<PoolTaskHandle<?>>[] taskBuckets;

    private TaskScheduleWorker scheduleWorker;
    private ConcurrentLinkedQueue<Thread> poolTerminateWaitQueue;

    //***************************************************************************************************************//
    //                                          1: pool initialization(1+1)                                          //
    //***************************************************************************************************************//
    public void init(TaskServiceConfig config) throws TaskPoolException, TaskServiceConfigException {
        if (config == null) throw new PoolInitializedException("Pool configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {
            try {
                startup(config.check());
                this.poolState = POOL_RUNNING;//ready to accept submitted tasks
            } catch (Throwable e) {
                this.poolState = POOL_NEW;//reset pool state to new when initializes fail
                throw e;
            }
        } else {
            throw new PoolInitializedException("Pool has been initialized");
        }
    }

    private void startup(TaskServiceConfig config) {
        this.monitorVo = new PoolMonitorVo();
        this.maxTaskSize = config.getMaxTaskSize();
        this.maxScheduleTaskSize = config.getMaxScheduleTaskSize();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        long keepAliveTimeNanos = MILLISECONDS.toNanos(config.getWorkerKeepAliveTime());
        boolean useTimePark = keepAliveTimeNanos > 0L;
        int workerSpins = useTimePark ? maxTimedSpins : maxUntimedSpins;

        this.workerSize = config.getWorkerSize();
        this.maxNoOfWorkers = workerSize - 1;
        this.workers = new TaskExecutionWorker[workerSize];
        this.taskBuckets = new ConcurrentLinkedQueue[workerSize];

        TaskPoolThreadFactory threadFactory = config.getThreadFactory();
        this.scheduleWorker = new TaskScheduleWorker(threadFactory, this);
        this.notifier = new TaskExecutionNotifier(threadFactory, keepAliveTimeNanos, useTimePark, workerSpins, workers);
        for (int i = 0; i < workerSize; i++) {
            taskBuckets[i] = new ConcurrentLinkedQueue<>();
            workers[i] = new TaskExecutionWorker(threadFactory, keepAliveTimeNanos, useTimePark, workerSpins, taskBuckets[i], taskBuckets);
        }
    }

    //***************************************************************************************************************//
    //                                       2: task submission(6+5)                                                 //
    //***************************************************************************************************************//
    public <V> TaskHandle<V> submit(Task<V> task) throws TaskException {
        return submit(task, (TaskAspect<V>) null);
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskAspect<V> aspect) throws TaskException {
        this.checkSubmittedExecTask(task);

        PoolTaskHandle<V> handle = new PoolTaskHandle<>(task, aspect, this, true);
        this.pushToTaskBucket(handle, false);
        return handle;
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> operator) throws TaskException {
        return submit(task, operator, null);
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> operator, TaskAspect<V> aspect) throws TaskException {
        if (operator == null) throw new TaskException("Task join operator can't be null");
        this.checkSubmittedExecTask(task);

        PoolTaskHandle<V> handle = new JoinTaskHandle<>(task, operator, aspect, this);
        this.pushToTaskBucket(handle, false);
        return handle;
    }

    public <V> TaskHandle<V> submit(TreeLayerTask<V> task) throws TaskException {
        return submit(task, null);
    }

    public <V> TaskHandle<V> submit(TreeLayerTask<V> task, TaskAspect<V> aspect) throws TaskException {
        this.checkSubmittedExecTask(task);

        TreeLayerTaskHandle<V> handle = new TreeLayerTaskHandle<>(task, aspect, this);
        this.pushToTaskBucket(handle, false);
        return handle;
    }

    private void checkSubmittedExecTask(Object task) throws TaskException {
        if (task == null) throw new TaskException("Task can't be null");
        if (poolState != POOL_RUNNING) throw new TaskRejectedException("Pool has been closed or in clearing");

        int curCount;
        do {
            curCount = taskCount;
            if (curCount >= maxTaskSize) throw new TaskRejectedException("Pool task count has reach max size");
        } while (!TaskCountUpd.compareAndSet(this, curCount, curCount + 1));
    }

    void decrementTaskCount() {
        int curCount;
        do {
            curCount = taskCount;
            if (curCount == 0) return;
        } while (!TaskCountUpd.compareAndSet(this, curCount, curCount - 1));
    }

    boolean incrementTaskCountForInternal(int addCount) {//call for join task,tree tasks and schedule tasks
        int curCount, newCount;
        do {
            curCount = taskCount;
            newCount = curCount + addCount;

            if (newCount <= 0) return false;//Task count exceeded
        } while (!TaskCountUpd.compareAndSet(this, curCount, newCount));
        return true;
    }

    //push a task handle to execution worker
    void pushToTaskBucket(PoolTaskHandle<?> taskHandle, boolean isScheduledTask) {
        if (isScheduledTask) {
            int arrayIndex = -1;
            for (int i = 0; i < workerSize; i++) {
                if (!workers[i].isRunning()) {
                    arrayIndex = i;
                    break;
                }
            }

            if (arrayIndex == -1) arrayIndex = this.maxNoOfWorkers & taskHandle.hashCode();
            ConcurrentLinkedQueue<PoolTaskHandle<?>> bucket = this.taskBuckets[arrayIndex];
            bucket.offer(taskHandle);
            taskHandle.setTaskBucket(bucket);
            workers[arrayIndex].activate();
        } else {
            int threadHashCode = (int) Thread.currentThread().getId();
            int arrayIndex = this.maxNoOfWorkers & (threadHashCode ^ (threadHashCode >>> 16));
            ConcurrentLinkedQueue<PoolTaskHandle<?>> bucket = this.taskBuckets[arrayIndex];
            bucket.offer(taskHandle);
            taskHandle.setTaskBucket(bucket);

            if (taskCount <= workerSize) {
                workers[arrayIndex].activate();
            } else {
                notifier.activate();
            }
        }
    }

    void attemptActivateAllWorkers() {
        if (taskCount > workerSize) notifier.activate();
    }

    //***************************************************************************************************************//
    //                                    3: Schedule task submit(6+2)                                               //
    //***************************************************************************************************************//
    public <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, delay, 0L, false, null, 1);
    }

    public <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, null, 2);
    }

    public <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, null, 3);
    }

    public <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit, TaskAspect<V> aspect) throws TaskException {
        return addScheduleTask(task, unit, delay, 0L, false, aspect, 1);
    }

    public <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit, TaskAspect<V> aspect) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, aspect, 2);
    }

    public <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit, TaskAspect<V> aspect) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, aspect, 3);
    }

    void decrementScheduledTaskCount() {
        int curCount;
        do {
            curCount = scheduledTaskCount;
            if (curCount == 0) return;
        } while (!ScheduledTaskCountUpd.compareAndSet(this, curCount, curCount - 1));
    }

    private <V> TaskScheduledHandle<V> addScheduleTask(Task<V> task, TimeUnit unit, long initialDelay, long intervalTime, boolean fixedDelay, TaskAspect<V> aspect, int scheduledType) throws TaskException {
        //1: check time
        if (poolState != POOL_RUNNING) throw new TaskRejectedException("Pool has been closed or in clearing");
        if (task == null) throw new TaskException("Task can't be null");
        if (unit == null) throw new TaskException("Time unit can't be null");
        if (initialDelay < 0L)
            throw new TaskException(scheduledType == 1 ? "Delay" : "Initial delay" + " time can't be less than zero");
        if (intervalTime <= 0L && scheduledType != 1)
            throw new TaskException(scheduledType == 2 ? "Period" : "Delay" + " time must be greater than zero");

        int curCount;
        do {
            curCount = scheduledTaskCount;
            if (curCount == maxScheduleTaskSize)
                throw new TaskRejectedException("Pool scheduled task count has reach max size");
        } while (!ScheduledTaskCountUpd.compareAndSet(this, curCount, curCount + 1));

        //3: create task handle and put it to schedule worker
        long intervalNanos = unit.toNanos(intervalTime);
        long firstRunNanos = unit.toNanos(initialDelay) + System.nanoTime();
        ScheduledTaskHandle<V> handle = new ScheduledTaskHandle<>(task, aspect, firstRunNanos, intervalNanos, fixedDelay, scheduleWorker, this);
        scheduleWorker.put(handle);

        //4: return this handle to method caller
        return handle;
    }

    //***************************************************************************************************************//
    //                                    4: some query methods(5+4)                                                 //
    //***************************************************************************************************************//
    int getPoolState() {
        return this.poolState;
    }

    public boolean isTerminated() {
        return poolState == POOL_TERMINATED;
    }

    public boolean isTerminating() {
        return poolState == POOL_TERMINATING;
    }

    public int getRunningCount() {
        int count = 0;
        for (TaskExecutionWorker worker : workers)
            if (worker.getProcessingHandle() != null) count++;
        return count;
    }

    public long getCompletedCount() {
        long count = 0;
        for (TaskExecutionWorker worker : workers)
            count += worker.getCompletedCount();

        return count;
    }

    public PoolMonitorVo getPoolMonitorVo() {
        monitorVo.setPoolState(this.poolState);
        monitorVo.setTaskCount(taskCount);
        monitorVo.setTaskRunningCount(getRunningCount());
        monitorVo.setTaskCompletedCount(getCompletedCount());
        monitorVo.setScheduledTaskCount(scheduledTaskCount);
        return monitorVo;
    }

    //***************************************************************************************************************//
    //                                      5: Pool clear(2+2)                                                       //
    //***************************************************************************************************************//
    public boolean clear(boolean mayInterruptIfRunning) throws TaskServiceConfigException {
        return clear(mayInterruptIfRunning, null);
    }

    public boolean clear(boolean mayInterruptIfRunning, TaskServiceConfig config) throws TaskServiceConfigException {
        return false;
    }

    private TaskPoolTerminatedVo removeAll(boolean mayInterruptIfRunning) {
        List<PoolTaskHandle<?>> unCompletedHandleList = scheduleWorker.getUnCompletedTasks();
        for (TaskExecutionWorker worker : workers) {
            worker.passivate(mayInterruptIfRunning);
        }
        return null;//@todo to be implemented
    }

    //***************************************************************************************************************//
    //                                      6: Pool shutdown (4)                                                     //
    //***************************************************************************************************************//
    public TaskPoolTerminatedVo terminate(boolean mayInterruptIfRunning) throws TaskPoolException {
        int state = this.poolState;
        if (state == POOL_TERMINATED)
            throw new TaskPoolException("Pool has been terminated");
        if (state == POOL_TERMINATING)
            throw new TaskPoolException("Operation failed,a termination process has been in executing");

        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_TERMINATING)) {
            TaskPoolTerminatedVo info = this.removeAll(mayInterruptIfRunning);

            scheduleWorker.passivate(mayInterruptIfRunning);
            for (TaskExecutionWorker worker : workers)
                worker.passivate(mayInterruptIfRunning);

            for (Thread thread : poolTerminateWaitQueue)
                LockSupport.unpark(thread);

            this.poolState = POOL_TERMINATED;
            return info;
        } else {
            throw new TaskPoolException("Operation failed,a termination process has been in executing");
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (this.poolState == POOL_TERMINATED) return true;

        boolean timed = timeout > 0L;
        if (timed && unit == null) throw new IllegalArgumentException("Time unit can't be null");

        Thread currentThread = Thread.currentThread();
        poolTerminateWaitQueue.offer(currentThread);
        long deadline = timed ? System.nanoTime() + unit.toNanos(timeout) : 0L;

        try {
            do {
                if (this.poolState == POOL_TERMINATED) return true;

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
}
