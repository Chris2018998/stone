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
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beetp.pool.PoolConstants.*;

/**
 * Task Pool Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class PoolTaskCenter implements TaskPool {
    private static final AtomicIntegerFieldUpdater<PoolTaskCenter> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(PoolTaskCenter.class, "poolState");
    private String poolName;
    private volatile int poolState;

    private int maxTaskSize;
    private LongAdder taskCount;//(count of once tasks + count of scheduled tasks + root count of joined tasks)
    private LongAdder runningCount;
    private LongAdder completedCount;

    private long idleTimeoutNanos;
    private boolean idleTimeoutValid;
    private int executeWorkersIndexHashBase;
    private TaskExecuteWorker[] executeWorkers;

    private PoolMonitorVo monitorVo;
    private TaskScheduleWorker scheduleWorker;
    private ConcurrentLinkedQueue<Thread> poolTerminateWaitQueue;

    //***************************************************************************************************************//
    //                                          1: pool initialization(2)                                            //
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
        //step1: copy values of some configured items to pool local fields
        this.poolName = config.getPoolName();
        this.maxTaskSize = config.getMaxTaskSize();
        this.idleTimeoutNanos = MILLISECONDS.toNanos(config.getWorkerKeepAliveTime());
        this.idleTimeoutValid = this.idleTimeoutNanos > 0L;

        //step2: create some runtime objects
        this.taskCount = new LongAdder();
        this.runningCount = new LongAdder();
        this.completedCount = new LongAdder();
        this.monitorVo = new PoolMonitorVo();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        //step3: create workers array and fill workers full size
        int workerSize = config.getWorkerSize();
        this.executeWorkersIndexHashBase = workerSize - 1;
        this.executeWorkers = new TaskExecuteWorker[workerSize];
        for (int i = 0; i < workerSize; i++)
            executeWorkers[i] = new TaskExecuteWorker(this);

        //step4: create daemon thread to poll expired tasks from scheduled queue and assign to workers
        if (scheduleWorker == null) this.scheduleWorker = new TaskScheduleWorker(this);
    }

    //***************************************************************************************************************//
    //                                       2: task submission(6)                                                   //
    //***************************************************************************************************************//
    public <V> TaskHandle<V> submit(Task<V> task) throws TaskException {
        return submit(task, (TaskAspect<V>) null);
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskAspect<V> aspect) throws TaskException {
        this.checkSubmittedTask(task);

        PoolTaskHandle<V> handle = new PoolTaskHandle<>(task, aspect, this, true);
        this.pushToExecuteWorker(handle);
        return handle;
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> operator) throws TaskException {
        return submit(task, operator, null);
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> operator, TaskAspect<V> aspect) throws TaskException {
        if (operator == null) throw new TaskException("Task join operator can't be null");
        this.checkSubmittedTask(task);

        PoolTaskHandle<V> handle = new JoinTaskHandle<>(task, operator, aspect, this);
        this.pushToExecuteWorker(handle);
        return handle;
    }

    public <V> TaskHandle<V> submit(TreeLayerTask<V> task) throws TaskException {
        return submit(task, null);
    }

    public <V> TaskHandle<V> submit(TreeLayerTask<V> task, TaskAspect<V> aspect) throws TaskException {
        this.checkSubmittedTask(task);

        TreeLayerTaskHandle<V> handle = new TreeLayerTaskHandle<>(task, aspect, this);
        this.pushToExecuteWorker(handle);
        return handle;
    }

    //***************************************************************************************************************//
    //                                    3: Scheduled task submit(6)                                                //
    //***************************************************************************************************************//
    public <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, delay, 0, false, null, 1);
    }

    public <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, null, 2);
    }

    public <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, null, 3);
    }

    public <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit, TaskAspect<V> aspect) throws TaskException {
        return addScheduleTask(task, unit, delay, 0, false, aspect, 1);
    }

    public <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit, TaskAspect<V> aspect) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, aspect, 2);
    }

    public <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit, TaskAspect<V> aspect) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, aspect, 3);
    }

    //***************************************************************************************************************//
    //                                  4: check submitted tasks and offer them                                       //
    //***************************************************************************************************************//
    private void checkSubmittedTask(Object task) throws TaskException {
        if (task == null) throw new TaskException("Task can't be null");
        if (poolState != POOL_RUNNING) throw new TaskRejectedException("Pool has been closed or in clearing");
        if (taskCount.sum() >= maxTaskSize) throw new TaskRejectedException("Task count has reach max capacity");

        taskCount.increment();
    }

    //push task to execution queue(**scheduled peek thread calls this method to push task**)
    void pushToExecuteWorker(PoolTaskHandle<?> taskHandle) {
        int threadHashCode = Thread.currentThread().hashCode();
        int arrayIndex = this.executeWorkersIndexHashBase & (threadHashCode ^ (threadHashCode >>> 16));
        this.executeWorkers[arrayIndex].put(taskHandle);
    }

    private <V> TaskScheduledHandle<V> addScheduleTask(Task<V> task, TimeUnit unit, long initialDelay, long intervalTime, boolean fixedDelay, TaskAspect<V> aspect, int scheduledType) throws TaskException {
        //1: check task
        if (task == null) throw new TaskException("Task can't be null");
        if (unit == null) throw new TaskException("Task time unit can't be null");
        if (initialDelay < 0)
            throw new TaskException(scheduledType == 1 ? "Delay" : "Initial delay" + " time can't be less than zero");
        if (intervalTime <= 0 && scheduledType != 1)
            throw new TaskException(scheduledType == 2 ? "Period" : "Delay" + " time must be greater than zero");

        //2: check pool state and task capacity
        this.checkSubmittedTask(task);

        //3: create task handle
        long intervalNanos = unit.toNanos(intervalTime);
        long firstRunNanos = unit.toNanos(initialDelay) + System.nanoTime();
        PoolTimedTaskHandle<V> handle = new PoolTimedTaskHandle<>(task, aspect, firstRunNanos, intervalNanos, fixedDelay, this);

        //4: add task handle to time sortable array,and gets its index in array
        scheduleWorker.put(handle);
        return handle;
    }

    //***************************************************************************************************************//
    //                                      5: Pool clear/remove(3)                                                  //
    //***************************************************************************************************************//
    public boolean clear(boolean mayInterruptIfRunning) {
        try {
            return clear(mayInterruptIfRunning, null);
        } catch (TaskServiceConfigException e) {
            return false;
        }
    }

    public boolean clear(boolean mayInterruptIfRunning, TaskServiceConfig config) throws TaskServiceConfigException {
        return false;
    }

    private TaskPoolTerminatedVo removeAll(boolean mayInterruptIfRunning) {
        List<PoolTaskHandle<?>> unCompletedHandleList = scheduleWorker.terminate();
        for (TaskExecuteWorker worker : executeWorkers) {
            worker.terminate();
        }

        return null;//@todo to be implemented
    }

    //remove from array or queue(method called inside handle)
    void removeCancelledTask(PoolTaskHandle handle) {
//        if (handle instanceof ScheduledTaskHandle) {
//            int taskIndex = scheduledDelayedQueue.remove((ScheduledTaskHandle) handle);
//            if (taskIndex >= 0) taskCount.decrementAndGet();//task removed successfully by call thread
//            if (taskIndex == 0) wakeupSchedulePeekThread();
//        } else if (taskQueue.remove(handle) && handle.isRoot) {
//            taskCount.decrementAndGet();
//        }
    }

    //***************************************************************************************************************//
    //                                      6: Pool shutdown (4)                                                     //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return poolState == POOL_TERMINATED;
    }

    public boolean isTerminating() {
        return poolState == POOL_TERMINATING;
    }

    public TaskPoolTerminatedVo terminate(boolean mayInterruptIfRunning) throws TaskPoolException {
        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_TERMINATING)) {
            TaskPoolTerminatedVo info = this.removeAll(mayInterruptIfRunning);

            scheduleWorker.terminate();
            for (TaskExecuteWorker worker : executeWorkers)
                worker.terminate();

            for (Thread thread : poolTerminateWaitQueue)
                LockSupport.unpark(thread);

            this.poolState = POOL_TERMINATED;
            return info;
        } else {
            throw new TaskPoolException("Termination forbidden,pool has been in terminating or afterTerminated");
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (this.poolState == POOL_TERMINATED) return true;
        if (timeout < 0) throw new IllegalArgumentException("Time out value must be greater than zero");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");

        Thread currentThread = Thread.currentThread();
        poolTerminateWaitQueue.offer(currentThread);
        long timeoutNano = unit.toNanos(timeout);
        boolean timed = timeoutNano > 0;
        long deadline = System.nanoTime() + timeoutNano;

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

    //***************************************************************************************************************//
    //                                     7: Pool monitor(1)                                                        //
    //***************************************************************************************************************//


    public PoolMonitorVo getPoolMonitorVo() {

//        monitorVo.setPoolState(this.poolState);
//        monitorVo.setWorkerCount(executeWorkers.length);
//        monitorVo.setTaskHoldingCount(taskCount.get());
//        int runningCount = 0;
//        List<PoolTaskHandle> runningTasks = new ArrayList<>(10);
//        for (TaskExecuteWorker worker : executeWorkers) {
//            completedCount += worker.completedCount;
//            PoolTaskHandle curTaskHandle = worker.curTaskHandle;
//            if (curTaskHandle != null) {
//                PoolTaskHandle rootHandle = null;
//                if (curTaskHandle.isRoot) {
//                    rootHandle = curTaskHandle;
//                } else if (curTaskHandle instanceof JoinTaskHandle) {
//                    rootHandle = ((JoinTaskHandle) curTaskHandle).root;
//                } else if (curTaskHandle instanceof TreeLayerTaskHandle) {
//                    rootHandle = ((TreeLayerTaskHandle) curTaskHandle).root;
//                }
//
//                if (!runningTasks.contains(rootHandle)) {
//                    runningTasks.add(rootHandle);
//                    runningCount++;
//                }
//            }
//        }

//        monitorVo.setTaskRunningCount(runningCount);
        monitorVo.setTaskCompletedCount(completedCount.sum());
        return monitorVo;
    }

    public int getPoolState() {
        return this.poolState;
    }

    public LongAdder getTaskCount() {
        return taskCount;
    }

    public LongAdder getRunningCount() {
        return runningCount;
    }

    public LongAdder getCompletedCount() {
        return completedCount;
    }

    public long getIdleTimeoutNanos() {
        return this.idleTimeoutNanos;
    }

    public boolean isIdleTimeoutValid() {
        return this.idleTimeoutValid;
    }

    public TaskExecuteWorker[] getExecuteWorkers() {
        return this.executeWorkers;
    }

}
