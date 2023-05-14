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
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.beetp.pool.exception.TaskRejectedException;
import org.stone.util.SortedArray;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beetp.pool.TaskPoolStaticUtil.*;

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
    //*part1:control parameters
    private int maxTaskSize;
    private int maxWorkerSize;
    private boolean workerInDaemon;
    private long workerKeepAliveTime;
    private boolean workerKeepaliveTimed;
    //*part2:atomic numbers of pool monitor
    private AtomicInteger workerCount;
    //number of tasks in queue and array(once count + scheduled count)
    private AtomicInteger taskWaitingCount;
    private AtomicLong taskRunningCount;
    private AtomicLong taskCompletedCount;
    private TaskPoolMonitorVo monitorVo;
    //*part3:queues of task execution
    private AtomicInteger workerNameIndex;
    private ConcurrentLinkedQueue<PoolWorkerThread> workerQueue;
    private ConcurrentLinkedQueue<TaskExecuteHandle> executionQueue;

    //*part4:task schedule
    //store sortable scheduled tasks
    private SortedArray<TaskScheduledHandle> scheduledArray;
    //a daemon thread park on first task util it expired,then push it to execution queue
    private PoolScheduledTaskPeekThread scheduledTaskPeekThread;
    //*part5:wait queue for pool shutting down
    private ConcurrentLinkedQueue<Thread> poolTerminateWaitQueue;

    //***************************************************************************************************************//
    //                1: pool initialization(1)                                                                      //
    //***************************************************************************************************************//
    public void init(BeeTaskServiceConfig config) throws BeeTaskPoolException, BeeTaskServiceConfigException {
        //step1: pool config check
        if (config == null) throw new PoolInitializedException("Pool configuration can't be null");
        BeeTaskServiceConfig checkedConfig = config.check();

        //step2: simple attribute setting
        this.poolName = checkedConfig.getPoolName();
        this.maxTaskSize = checkedConfig.getTaskMaxSize();
        this.maxWorkerSize = checkedConfig.getMaxWorkerSize();
        this.workerInDaemon = checkedConfig.isWorkInDaemon();
        this.workerKeepAliveTime = MILLISECONDS.toNanos(checkedConfig.getWorkerKeepAliveTime());
        this.workerKeepaliveTimed = workerKeepAliveTime > 0;

        //step3: create queues
        this.workerNameIndex = new AtomicInteger(1);
        this.workerQueue = new ConcurrentLinkedQueue<>();
        this.executionQueue = new ConcurrentLinkedQueue<>();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        //step4: atomic fields of pool monitor
        this.monitorVo = new TaskPoolMonitorVo();
        this.taskWaitingCount = new AtomicInteger();
        this.workerCount = new AtomicInteger();
        this.taskRunningCount = new AtomicLong();
        this.taskCompletedCount = new AtomicLong();

        //step5: create initial worker threads by config
        int workerInitSize = config.getInitWorkerSize();
        this.workerCount.set(workerInitSize);
        for (int i = 0; i < workerInitSize; i++) {
            PoolWorkerThread worker = new PoolWorkerThread(WORKER_WORKING);
            workerQueue.offer(worker);
            worker.start();
        }

        //step6: create task schedule
        this.scheduledArray = new SortedArray<>(TaskScheduledHandle.class, 0,
                new Comparator<TaskScheduledHandle>() {
                    public int compare(TaskScheduledHandle handle1, TaskScheduledHandle handle2) {
                        long compareV = handle1.getNextTime() - handle2.getNextTime();
                        if (compareV > 0) return 1;
                        if (compareV == 0) return 0;
                        return -1;
                    }
                });

        this.scheduledTaskPeekThread = new PoolScheduledTaskPeekThread();
        this.scheduledTaskPeekThread.start();

        //step7: set pool to be running state(ready to accept tasks)
        this.poolState = POOL_RUNNING;
    }

    //***************************************************************************************************************//
    //                2: task submit(2)                                                                              //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException {
        return this.submit(task, null);
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException {
        //1:task entrance test
        taskCheck(task, false, 0, 0, 0, null);
        //2:create handle and push it to execution queue
        TaskExecuteHandle handle = new TaskExecuteHandle(task, callback, this);
        this.pushToExecutionQueue(handle);
        return handle;
    }

    //***************************************************************************************************************//
    //                3: task schedule(6)                                                                            //
    //***************************************************************************************************************//
    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException {
        return this.schedule(task, delay, unit, null);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException {
        return this.scheduleAtFixedRate(task, initialDelay, period, unit, null);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit) throws BeeTaskException {
        return this.scheduleWithFixedDelay(task, initialDelay, delay, unit, null);
    }

    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        //1:task entrance test
        taskCheck(task, true, 1, delay, 0, unit);
        //2:add a schedule handle to pool
        long firstTime = System.nanoTime() + unit.toNanos(delay);
        return addScheduleTask(task, callback, firstTime, 0, false);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        //1:task entrance test
        taskCheck(task, true, 2, initialDelay, period, unit);
        //2:add a schedule handle to pool
        long firstTime = System.nanoTime() + unit.toNanos(initialDelay);
        return addScheduleTask(task, callback, firstTime, unit.toNanos(period), false);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        //1:task entrance test
        taskCheck(task, true, 2, initialDelay, delay, unit);
        //2:add a schedule handle to pool
        long firstTime = System.nanoTime() + unit.toNanos(initialDelay);
        return addScheduleTask(task, callback, firstTime, unit.toNanos(delay), true);
    }

    private TaskScheduledHandle addScheduleTask(BeeTask task, BeeTaskCallback callback, long firstTime, long intervalTime, boolean fixedDelay) throws BeeTaskException {
        TaskScheduledHandle handle = new TaskScheduledHandle(task, callback, this, firstTime, intervalTime, fixedDelay);

        int index = scheduledArray.add(handle);
        if (this.poolState != POOL_RUNNING) {//recheck pool state,if shutdown,then cancel task
            if (handle.setAsCancelled()) {
                if (scheduledArray.remove(handle) >= 0) taskWaitingCount.decrementAndGet();
                throw new TaskRejectedException("Access forbidden,task pool was closed or in clearing");
            }
        }

        if (index == 0) wakeupSchedulePeekThread();
        return handle;
    }

    //***************************************************************************************************************//
    //                3: task enter(3)                                                                               //                                                                                  //
    //***************************************************************************************************************//
    private void taskCheck(BeeTask task, boolean scheduleInd, int scheduleType,
                           long initialDelay, long intervalTime, TimeUnit unit) throws BeeTaskException {
        //1: task test
        if (task == null) throw new BeeTaskException("Task can't be null");
        if (scheduleInd) {
            if (unit == null) throw new BeeTaskException("Time unit can't be null");
            if (initialDelay < 0)
                throw new BeeTaskException(scheduleType == 1 ? "Delay" : "Initial delay" + " time can't be less than zero");
            if (intervalTime <= 0 && scheduleType != 1)
                throw new BeeTaskException(scheduleType == 2 ? "Period" : "Delay" + " time must be greater than zero");
        }

        //2: pool state check
        if (this.poolState != POOL_RUNNING)
            throw new TaskRejectedException("Access forbidden,task pool was closed or in clearing");

        //3: try to increment task count,failed,throws exception
        do {
            int currentCount = taskWaitingCount.get();
            if (currentCount >= maxTaskSize) throw new TaskRejectedException("Pool was full,task rejected");
            if (taskWaitingCount.compareAndSet(currentCount, currentCount + 1)) return;
        } while (true);
    }

    //push task to execution queue(**scheduled peek thread call this method to push task**)
    private void pushToExecutionQueue(TaskExecuteHandle taskHandle) {
        //1:try to wakeup a idle worker to handle the task
        for (PoolWorkerThread workerThread : workerQueue) {
            if (workerThread.compareAndSetState(WORKER_IDLE, taskHandle)) {
                LockSupport.unpark(workerThread);
                return;
            }
        }

        //2:try to create a new worker to handle it
        do {
            int currentCount = this.workerCount.get();
            //2.1: worker current count reach max size
            if (currentCount >= this.maxWorkerSize) {
                executionQueue.offer(taskHandle);
                return;
            }

            //2.2: create new worker with the task handle
            if (workerCount.compareAndSet(currentCount, currentCount + 1)) {
                PoolWorkerThread worker = new PoolWorkerThread(taskHandle);
                workerQueue.offer(worker);
                worker.start();
                return;
            }
        } while (true);
    }

    //remove from array or queue
    void removeCancelledTask(TaskExecuteHandle handle) {
        if (handle instanceof TaskScheduledHandle) {
            int taskIndex = scheduledArray.remove((TaskScheduledHandle) handle);
            if (taskIndex >= 0) taskWaitingCount.decrementAndGet();
            if (taskIndex == 0) wakeupSchedulePeekThread();
        } else if (executionQueue.remove(handle)) {
            taskWaitingCount.decrementAndGet();
        }
    }

    //***************************************************************************************************************//
    //                4: Execute Task(1)                                                                             //
    //***************************************************************************************************************//
    private void executeTask(TaskExecuteHandle handle) {
        try {
            //1: increment running count
            taskRunningCount.incrementAndGet();//need think of count exceeded?

            //2: execute callback
            BeeTask task = handle.getTask();
            BeeTaskCallback callback = handle.getCallback();
            if (callback != null) {
                try {
                    callback.beforeCall(handle);
                } catch (Throwable e) {
                    //do nothing
                }
            }

            //3: execute task
            try {
                handle.setDone(TASK_RESULT, task.call());
            } catch (Throwable e) {
                handle.setDone(TASK_EXCEPTION, new TaskExecutionException(e));
            }
        } finally {
            taskRunningCount.decrementAndGet();
            if (handle instanceof TaskScheduledHandle) {
                TaskScheduledHandle scheduledHandle = (TaskScheduledHandle) handle;
                if (scheduledHandle.isPeriodic()) {
                    scheduledHandle.prepareForNextCall();//reset to waiting state for next execution
                    if (scheduledArray.add(scheduledHandle) == 0) {
                        wakeupSchedulePeekThread();
                    }
                } else {//one timed task,so end
                    taskCompletedCount.incrementAndGet();
                }
            } else {
                taskCompletedCount.incrementAndGet();
            }
        }
    }

    private void wakeupSchedulePeekThread() {
        LockSupport.unpark(scheduledTaskPeekThread);
    }

    //***************************************************************************************************************//
    //                5: Pool clear (1)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    public boolean clear(boolean mayInterruptIfRunning) {
        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_CLEARING)) {
            TaskExecuteHandle taskHandle;
            while ((taskHandle = executionQueue.poll()) != null) {
                taskHandle.setDone(TASK_CANCELLED, null);
            }

            PoolWorkerThread worker;
            if (mayInterruptIfRunning) {
                while ((worker = workerQueue.poll()) != null) {
                    worker.interrupt();
                }
            } else {
                while ((worker = workerQueue.poll()) != null) {
                    try {
                        worker.join();
                    } catch (Exception e) {
                        //do nothing
                    }
                }
            }

            this.poolState = POOL_RUNNING;
            return true;
        } else {
            return false;
        }
    }

    //***************************************************************************************************************//
    //                6: Pool shutdown (4)                                                                           //                                                                                  //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return poolState == POOL_TERMINATED;
    }

    public boolean isTerminating() {
        return poolState == POOL_TERMINATING;
    }

    public List<BeeTask> terminate(boolean mayInterruptIfRunning) throws BeeTaskPoolException {
        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_TERMINATING)) {
            PoolWorkerThread worker;
            if (mayInterruptIfRunning) {
                while ((worker = workerQueue.poll()) != null) {
                    worker.interrupt();
                }
            } else {
                while ((worker = workerQueue.poll()) != null) {
                    try {
                        worker.join();
                    } catch (Exception e) {
                        //do nothing
                    }
                }
            }

            TaskExecuteHandle taskHandle;
            List<BeeTask> queueTaskList = new LinkedList<>();
            while ((taskHandle = executionQueue.poll()) != null) {
                queueTaskList.add(taskHandle.getTask());
                taskHandle.setDone(TASK_CANCELLED, null);
            }

            //set
            this.poolState = POOL_TERMINATED;
            for (Thread thread : poolTerminateWaitQueue)
                LockSupport.unpark(thread);
            return queueTaskList;
        } else {
            throw new BeeTaskPoolException("Termination forbidden,pool has been in terminating or afterTerminated");
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
    //                6: Pool monitor(1)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setWorkerCount(workerCount.get());
        monitorVo.setTaskWaitingCount(taskWaitingCount.get());
        monitorVo.setTaskRunningCount(taskRunningCount.get());
        monitorVo.setTaskCompletedCount(taskCompletedCount.get());
        return monitorVo;
    }

    //***************************************************************************************************************//
    //                7: Inner interfaces and classes (2)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //tasks execution thread
    private class PoolWorkerThread extends Thread {
        private final AtomicReference<Object> workState;

        PoolWorkerThread(Object state) {
            this.setDaemon(workerInDaemon);
            this.setName(poolName + "-worker thread-" + workerNameIndex.getAndIncrement());
            this.workState = new AtomicReference<>(state);
        }

        void setState(Object update) {
            workState.set(update);
        }

        boolean compareAndSetState(Object expect, Object update) {
            return workState.compareAndSet(expect, update);
        }

        public void run() {
            do {
                //1: read state from work
                Object state = workState.get();//exits repeat read same
                if (state == WORKER_TERMINATED || poolState >= POOL_TERMINATING)
                    break;

                //2: get task from state or poll from queue
                TaskExecuteHandle handle = null;
                if (state instanceof TaskExecuteHandle) {
                    handle = (TaskExecuteHandle) state;
                    state = WORKER_WORKING;
                    this.setState(WORKER_WORKING);
                }
                if (handle == null) handle = executionQueue.poll();

                //3: execute task
                if (handle != null) {
                    if (handle instanceof TaskScheduledHandle) {
                        TaskScheduledHandle scheduledHandle = (TaskScheduledHandle) handle;
                        if (!scheduledHandle.isPeriodic())
                            taskWaitingCount.decrementAndGet();
                    } else {
                        taskWaitingCount.decrementAndGet();
                    }

                    if (handle.setAsRunning()) executeTask(handle);
                } else if (compareAndSetState(state, WORKER_IDLE)) {//4: park work thread
                    if (workerKeepaliveTimed)
                        LockSupport.parkNanos(workerKeepAliveTime);
                    else
                        LockSupport.park();

                    compareAndSetState(WORKER_IDLE, WORKER_TERMINATED);
                }

                //try to clear interrupted status(maybe exists)
                Thread.interrupted();
            } while (true);

            workerCount.decrementAndGet();
            workerQueue.remove(this);
        }
    }

    //timed tasks peek thread
    private class PoolScheduledTaskPeekThread extends Thread {
        PoolScheduledTaskPeekThread() {
            this.setName(poolName + "-ScheduledPeek");
            this.setDaemon(true);
        }

        public void run() {
            while (poolState == POOL_RUNNING) {
                //1: peek first task from array
                TaskScheduledHandle taskHandle = scheduledArray.getFirst();

                if (taskHandle == null) {
                    //2: if task is null,then park without time
                    LockSupport.park();
                } else {
                    long parkTime = taskHandle.getNextTime() - System.nanoTime();
                    //3: if task is expired,then remove it and push it to execution queue
                    if (parkTime <= 0) {
                        scheduledArray.remove(taskHandle);
                        if (taskHandle.curState.get() == TASK_WAITING)
                            pushToExecutionQueue(taskHandle);//push it to execution queue
                    } else {
                        //4: if task is not expired,then park util expired
                        LockSupport.parkNanos(parkTime);
                    }
                }
            }
        }
    }
}
