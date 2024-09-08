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
import java.util.concurrent.atomic.AtomicInteger;
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
    //pool name
    private String poolName;
    //pool state can be changed via cas
    private volatile int poolState;
    //monitor vo of pool
    private PoolMonitorVo monitorVo;

    //max capacity of pool tasks
    private int maxTaskSize;
    //count of tasks in pool,its value =count of once tasks + count of scheduled tasks + root count of joined tasks
    private AtomicInteger taskCount;

    //spin size of worker to poll tasks
    private int workerSpins;
    private int executionWorkerSize;
    //base hash value for computing index of buckets
    private int maxSeqOfWorkerArray;
    //an internal thread to wake up all execution workers if task count greater than worker size
    private TaskInNotifyWorker notifyWorker;
    //an array of execution workers,it has fixed length
    private TaskExecuteWorker[] executeWorkers;
    //keep alive time of worker when no tasks to process
    private long keepAliveTimeNanos;
    //a worker to schedule timed tasks
    private TaskScheduleWorker scheduleWorker;
    //wait queue on pool termination
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
        //step1: copy values of some configured items to pool local fields
        this.poolName = config.getPoolName();
        this.maxTaskSize = config.getMaxTaskSize();
        this.keepAliveTimeNanos = MILLISECONDS.toNanos(config.getWorkerKeepAliveTime());
        this.workerSpins = this.keepAliveTimeNanos > 0L ? maxTimedSpins : maxUntimedSpins;

        //step2: create some runtime objects
        this.taskCount = new AtomicInteger();
        this.monitorVo = new PoolMonitorVo();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        //step3: create execution workers in inactive state
        this.executionWorkerSize = config.getWorkerSize();
        this.maxSeqOfWorkerArray = executionWorkerSize - 1;
        this.executeWorkers = new TaskExecuteWorker[executionWorkerSize];
        for (int i = 0; i < executionWorkerSize; i++)
            executeWorkers[i] = new TaskExecuteWorker(this);

        //step4: create notification worker
        this.notifyWorker = new TaskInNotifyWorker(this);

        //step5: create schedule worker to manage timed tasks
        if (scheduleWorker == null) this.scheduleWorker = new TaskScheduleWorker(this);
    }

    //***************************************************************************************************************//
    //                                       2: task submission(6+2)                                                 //
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

    private void checkSubmittedTask(Object task) throws TaskException {
        if (task == null) throw new TaskException("Task can't be null");
        if (poolState != POOL_RUNNING) throw new TaskRejectedException("Pool has been closed or in clearing");

        int curCount;
        do {
            curCount = taskCount.get();
            if (curCount == maxTaskSize) throw new TaskRejectedException("Pool task capacity has full");
        } while (!taskCount.compareAndSet(curCount, curCount + 1));
    }

    //push a task handle to execution worker
    void pushToExecuteWorker(PoolTaskHandle<?> taskHandle) {
        //1: compute index of worker by hash
        int threadHashCode = Thread.currentThread().hashCode();
        int arrayIndex = this.maxSeqOfWorkerArray & (threadHashCode ^ (threadHashCode >>> 16));
        TaskExecuteWorker worker = this.executeWorkers[arrayIndex];
        worker.put(taskHandle);//push this task to worker

        //2: wake up worker or all workers
        if (taskCount.get() < this.executionWorkerSize) {
            worker.wakeup();
        } else {//wake up all workers
            notifyWorker.wakeup();
        }
    }

    //***************************************************************************************************************//
    //                                    3: Timed task submit(6+1)                                                  //
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

    private <V> TaskScheduledHandle<V> addScheduleTask(Task<V> task, TimeUnit unit, long initialDelay, long intervalTime, boolean fixedDelay, TaskAspect<V> aspect, int scheduledType) throws TaskException {
        //1: check time
        if (unit == null) throw new TaskException("Time unit can't be null");
        if (initialDelay < 0)
            throw new TaskException(scheduledType == 1 ? "Delay" : "Initial delay" + " time can't be less than zero");
        if (intervalTime <= 0 && scheduledType != 1)
            throw new TaskException(scheduledType == 2 ? "Period" : "Delay" + " time must be greater than zero");

        //2: check task
        this.checkSubmittedTask(task);
        //3: create task handle and put it to schedule worker
        long intervalNanos = unit.toNanos(intervalTime);
        long firstRunNanos = unit.toNanos(initialDelay) + System.nanoTime();
        PoolTimedTaskHandle<V> handle = new PoolTimedTaskHandle<>(task, aspect, firstRunNanos, intervalNanos, fixedDelay, this);
        scheduleWorker.put(handle);

        //4: return this handle to method caller
        return handle;
    }

    //***************************************************************************************************************//
    //                                    4: some query methods(5+5)                                                 //
    //***************************************************************************************************************//
    int getPoolState() {
        return this.poolState;
    }

    AtomicInteger getTaskCount() {
        return taskCount;
    }

    int getWorkerSpins() {
        return this.workerSpins;
    }

    long getKeepAliveTimeNanos() {
        return this.keepAliveTimeNanos;
    }

    TaskExecuteWorker[] getExecuteWorkers() {
        return this.executeWorkers;
    }

    public boolean isTerminated() {
        return poolState == POOL_TERMINATED;
    }

    public boolean isTerminating() {
        return poolState == POOL_TERMINATING;
    }

    public int getRunningCount() {
        int count = 0;
        for (TaskExecuteWorker worker : executeWorkers)
            if (worker.getProcessingHandle() != null) count++;

        return count;
    }

    public long getCompletedCount() {
        long count = 0;
        for (TaskExecuteWorker worker : executeWorkers)
            count += worker.getCompletedCount();

        return count + scheduleWorker.getCompletedCount();
    }

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
        //monitorVo.setTaskCompletedCount(completedCount.sum());
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

    public TaskPoolTerminatedVo terminate(boolean mayInterruptIfRunning) throws TaskPoolException {
        int state = this.poolState;
        if (state == POOL_TERMINATED)
            throw new TaskPoolException("Pool has been terminated");
        if (state == POOL_TERMINATING)
            throw new TaskPoolException("Operation failed,a termination process has been in executing");

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
