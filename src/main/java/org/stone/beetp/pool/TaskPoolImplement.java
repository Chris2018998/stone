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
import org.stone.beetp.pool.exception.TaskRejectedException;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beetp.BeeTaskStates.TASK_CANCELLED;
import static org.stone.beetp.BeeTaskStates.TASK_WAITING;
import static org.stone.beetp.pool.TaskPoolConstants.*;

/**
 * Task Pool Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskPoolImplement implements BeeTaskPool {
    private static final AtomicIntegerFieldUpdater<TaskPoolImplement> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(TaskPoolImplement.class, "poolState");
    private String poolName;
    private volatile int poolState;

    private int maxTaskSize;
    private int maxWorkerSize;
    private boolean workerInDaemon;
    private long workerKeepAliveTime;
    private boolean workerKeepaliveTimed;

    private AtomicInteger workerCount;
    private AtomicInteger taskHoldingCount;//(once count + scheduled count + join count(root))
    private AtomicInteger taskRunningCount;
    private AtomicInteger taskCompletedCount;

    private TaskPoolMonitorVo monitorVo;
    private AtomicInteger workerNameIndex;
    private ConcurrentLinkedQueue<PoolWorkerThread> workerQueue;
    private ConcurrentLinkedQueue<BaseHandle> executionQueue;
    private ScheduledTaskQueue scheduledDelayedQueue;
    private PoolScheduledTaskPeekThread scheduledPeekThread;//wait at first task of scheduled queue util first task timeout,then poll it from queue
    private ConcurrentLinkedQueue<Thread> poolTerminateWaitQueue;

    //***************************************************************************************************************//
    //                                          1: pool initialization(1)                                            //
    //***************************************************************************************************************//
    public void init(BeeTaskServiceConfig config) throws BeeTaskPoolException, BeeTaskServiceConfigException {
        //step1: pool config check
        if (config == null) throw new PoolInitializedException("Pool configuration can't be null");
        BeeTaskServiceConfig checkedConfig = config.check();

        //step2: update pool state to running via cas
        if (!PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_RUNNING))
            throw new PoolInitializedException("Pool has been initialized");

        //step3: startup pool with a configuration object
        try {
            startup(checkedConfig);
            this.poolState = POOL_RUNNING;//ready to accept coming task submission
        } catch (Throwable e) {
            this.poolState = POOL_NEW;//reset to initial state when failed to startup
            throw e;
        }
    }

    private void startup(BeeTaskServiceConfig config) {
        //step1: copy config item to pool
        this.poolName = config.getPoolName();
        this.maxTaskSize = config.getTaskMaxSize();
        this.maxWorkerSize = config.getMaxWorkerSize();
        this.workerInDaemon = config.isWorkInDaemon();
        this.workerKeepAliveTime = MILLISECONDS.toNanos(config.getWorkerKeepAliveTime());
        this.workerKeepaliveTimed = workerKeepAliveTime > 0;

        //step2: create some queues(worker queue,task queue,termination wait queue)
        if (workerQueue == null) {
            this.workerNameIndex = new AtomicInteger();
            this.workerQueue = new ConcurrentLinkedQueue<>();
            this.executionQueue = new ConcurrentLinkedQueue<>();
            this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

            //step3: atomic fields of pool monitor
            this.monitorVo = new TaskPoolMonitorVo();
            this.workerCount = new AtomicInteger();
            this.taskHoldingCount = new AtomicInteger();
            this.taskRunningCount = new AtomicInteger();
            this.taskCompletedCount = new AtomicInteger();
        }

        //step4: create task execution threads
        int workerInitSize = config.getInitWorkerSize();
        this.workerCount.set(workerInitSize);
        for (int i = 0; i < workerInitSize; i++) {
            PoolWorkerThread worker = new PoolWorkerThread(WORKER_WORKING);
            workerQueue.offer(worker);
            worker.start();
        }

        //step5: create delayed queue and peek thread working on queue
        if (scheduledPeekThread == null) {
            this.scheduledDelayedQueue = new ScheduledTaskQueue(0);
            this.scheduledPeekThread = new PoolScheduledTaskPeekThread();
            this.scheduledPeekThread.start();
        }
    }

    //***************************************************************************************************************//
    //                                       2:  task submission(6)                                                  //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException {
        return submit(task, (BeeTaskCallback) null);
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskJoinOperator operator) throws BeeTaskException {
        return submit(task, operator, null);
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException {
        //1: check task
        if (task == null) throw new BeeTaskException("Task can't be null");
        //2: check pool state and pool space
        this.checkPool();

        //3: crete task handle
        BaseHandle handle = new BaseHandle(task, callback, true, this);
        //4: push task to execution queue
        this.pushToExecutionQueue(handle);
        //5: return handle
        return handle;
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskJoinOperator operator, BeeTaskCallback callback) throws BeeTaskException {
        //1: check task
        if (task == null) throw new BeeTaskException("Task can't be null");
        if (operator == null) throw new BeeTaskException("Task join operator can't be null");
        //2: check pool state and pool space
        this.checkPool();

        //3: crete join task handle(root)
        BaseHandle handle = new JoinTaskHandle(task, operator, callback, this);
        //4: push task to execution queue
        this.pushToExecutionQueue(handle);
        //5: return handle
        return handle;
    }

    public BeeTaskHandle submit(BeeTreeTask task) throws BeeTaskException {
        return submit(task, null);
    }

    public BeeTaskHandle submit(BeeTreeTask task, BeeTaskCallback callback) throws BeeTaskException {
        //1: check task
        if (task == null) throw new BeeTaskException("Task can't be null");
        //2: check pool state and pool space
        this.checkPool();

        //3: crete tree task handle(root)
        TreeTaskHandle handle = new TreeTaskHandle(task, callback, this);
        //4: push task to execution queue
        this.pushToExecutionQueue(handle);
        //5: return handle
        return handle;
    }

    //***************************************************************************************************************//
    //                                    3: Scheduled task submit(6)                                                //
    //***************************************************************************************************************//
    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException {
        return addScheduleTask(task, unit, delay, 0, false, null, 1);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, null, 2);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit) throws BeeTaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, null, 3);
    }

    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        return addScheduleTask(task, unit, delay, 0, false, callback, 1);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, callback, 2);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, callback, 3);
    }

    //***************************************************************************************************************//
    //                                  4: task check and task offer(4)                                              //
    //***************************************************************************************************************//
    private void checkPool() throws BeeTaskException {
        //1: pool state check
        if (this.poolState != POOL_RUNNING)
            throw new TaskRejectedException("Pool has been closed or in clearing");

        //2: task capacity full check
        do {
            int currentCount = taskHoldingCount.get();
            if (currentCount >= maxTaskSize) throw new TaskRejectedException("Capacity of tasks reached max size");
            if (taskHoldingCount.compareAndSet(currentCount, currentCount + 1)) return;
        } while (true);
    }

    //push task to execution queue(**scheduled peek thread calls this method to push task**)
    void pushToExecutionQueue(BaseHandle taskHandle) {
        //1:try to wakeup a idle work thread with task
        for (PoolWorkerThread workerThread : workerQueue) {
            if (workerThread.compareAndSetState(WORKER_IDLE, taskHandle)) {
                LockSupport.unpark(workerThread);
                return;
            }
        }

        do {
            //2.1: try to offer task to execution queue when worker count reach max size
            int curWorkerCount = this.workerCount.get();
            if (curWorkerCount >= this.maxWorkerSize) {
                executionQueue.offer(taskHandle);
                return;
            }

            //2.2: try to create a new worker thread to process this task
            if (workerCount.compareAndSet(curWorkerCount, curWorkerCount + 1)) {
                PoolWorkerThread worker = new PoolWorkerThread(taskHandle);
                workerQueue.offer(worker);
                worker.start();
                return;
            }
        } while (true);
    }

    void wakeupSchedulePeekThread() {
        LockSupport.unpark(scheduledPeekThread);
    }

    private BeeTaskScheduledHandle addScheduleTask(BeeTask task, TimeUnit unit, long initialDelay, long intervalTime, boolean fixedDelay, BeeTaskCallback callback, int scheduledType) throws BeeTaskException {
        //1: check task
        if (task == null) throw new BeeTaskException("Task can't be null");
        if (unit == null) throw new BeeTaskException("Task time unit can't be null");
        if (initialDelay < 0)
            throw new BeeTaskException(scheduledType == 1 ? "Delay" : "Initial delay" + " time can't be less than zero");
        if (intervalTime <= 0 && scheduledType != 1)
            throw new BeeTaskException(scheduledType == 2 ? "Period" : "Delay" + " time must be greater than zero");

        //2: check pool state and pool space
        this.checkPool();

        //3: create task handle
        long firstRunNanos = unit.toNanos(initialDelay);
        long intervalNanos = unit.toNanos(intervalTime);
        ScheduledTaskHandle handle = new ScheduledTaskHandle(task, callback, firstRunNanos, intervalNanos, fixedDelay, this);

        //4: add task handle to time sortable array,and gets its index in array
        int index = scheduledDelayedQueue.add(handle);

        //re-check pool state,if not in running,then try to cancel task
        if (this.poolState != POOL_RUNNING) {
            if (handle.setAsCancelled()) {
                if (scheduledDelayedQueue.remove(handle) >= 0) taskHoldingCount.decrementAndGet();
                throw new TaskRejectedException("Pool has been closed or in clearing");
            }
        }

        //if the task at first of scheduled queue,then wakeup the peek thread
        if (index == 0) wakeupSchedulePeekThread();
        return handle;
    }

    //***************************************************************************************************************//
    //                                      5: Pool clear/remove(3)                                                  //
    //***************************************************************************************************************//
    public boolean clear(boolean mayInterruptIfRunning) {
        try {
            return clear(mayInterruptIfRunning, null);
        } catch (BeeTaskServiceConfigException e) {
            return false;
        }
    }

    public boolean clear(boolean mayInterruptIfRunning, BeeTaskServiceConfig config) throws BeeTaskServiceConfigException {
        BeeTaskServiceConfig checkedConfig = null;
        if (config != null) checkedConfig = config.check();

        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_CLEARING)) {
            this.removeAll(mayInterruptIfRunning);
            if (checkedConfig != null) startup(checkedConfig);

            this.poolState = POOL_RUNNING;
            LockSupport.unpark(this.scheduledPeekThread);
            return true;
        } else {
            return false;
        }
    }

    private PoolCancelledTasks removeAll(boolean mayInterruptIfRunning) {
        List<BeeTask> unRunningTaskList = new LinkedList<>();
        List<BeeTreeTask> unRunningTreeTaskList = new LinkedList<>();

        //1: remove scheduled tasks
        for (ScheduledTaskHandle handle : scheduledDelayedQueue.clearAll()) {
            if (handle.setAsCancelled()) {//collect cancelled tasks by pool
                unRunningTaskList.add(handle.getTask());
                handle.setResult(TASK_CANCELLED, null);
            }
        }

        //2: remove tasks in execution queue(once tasks + join tasks)
        BaseHandle handle;
        while ((handle = executionQueue.poll()) != null) {
            if (handle.setAsCancelled()) {//collect cancelled tasks by pool
                if (handle instanceof TreeTaskHandle) {
                    unRunningTreeTaskList.add(((TreeTaskHandle) handle).getTreeTask());
                } else {
                    unRunningTaskList.add(handle.getTask());
                }

                handle.setResult(TASK_CANCELLED, null);
            }
        }

        //3: remove work threads
        for (PoolWorkerThread workerThread : workerQueue) {
            if (mayInterruptIfRunning) {
                workerThread.setState(WORKER_TERMINATED);
                workerThread.interrupt();
            }

            try {
                workerThread.join();
            } catch (Exception e) {
                //do nothing
            }
        }

        //4:reset atomic numbers to zero
        this.workerCount.set(0);
        this.taskHoldingCount.set(0);
        this.taskRunningCount.set(0);
        this.taskCompletedCount.set(0);
        this.workerNameIndex.set(0);
        return new PoolCancelledTasks(unRunningTaskList, unRunningTreeTaskList);
    }

    //remove from array or queue(method called inside handle)
    void removeCancelledTask(BaseHandle handle) {
        if (handle instanceof ScheduledTaskHandle) {
            int taskIndex = scheduledDelayedQueue.remove((ScheduledTaskHandle) handle);
            if (taskIndex >= 0) taskHoldingCount.decrementAndGet();//task removed successfully by call thread
            if (taskIndex == 0) wakeupSchedulePeekThread();
        } else if (executionQueue.remove(handle) && handle.isRoot()) {
            taskHoldingCount.decrementAndGet();
        }
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

    public PoolCancelledTasks terminate(boolean mayInterruptIfRunning) throws BeeTaskPoolException {
        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_TERMINATING)) {
            PoolCancelledTasks info = this.removeAll(mayInterruptIfRunning);

            this.poolState = POOL_TERMINATED;
            for (Thread thread : poolTerminateWaitQueue)
                LockSupport.unpark(thread);
            return info;
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
    //                                     7: Pool monitor(1)                                                        //
    //***************************************************************************************************************//
    AtomicInteger getTaskRunningCount() {
        return taskRunningCount;
    }

    AtomicInteger getTaskHoldingCount() {
        return this.taskHoldingCount;
    }

    AtomicInteger getTaskCompletedCount() {
        return taskCompletedCount;
    }

    ScheduledTaskQueue getScheduledDelayedQueue() {
        return scheduledDelayedQueue;
    }

    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setPoolState(this.poolState);
        monitorVo.setWorkerCount(workerCount.get());
        monitorVo.setTaskHoldingCount(taskHoldingCount.get());
        monitorVo.setTaskRunningCount(taskRunningCount.get());
        monitorVo.setTaskCompletedCount(taskCompletedCount.get());
        return monitorVo;
    }

    //***************************************************************************************************************//
    //                                  8: Pool worker class and scheduled class (2)                                 //
    //***************************************************************************************************************//
    private class PoolWorkerThread extends TaskWorkThread {
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
                BaseHandle handle;
                if (state instanceof BaseHandle) {
                    handle = (BaseHandle) state;
                    state = WORKER_WORKING;
                    this.workState.set(WORKER_WORKING);
                } else {
                    handle = executionQueue.poll();
                }

                //3: execute task
                if (handle != null) {
                    if (handle.setAsRunning(this)) {
                        this.currentTaskHandle = handle;//fix interrupt issue on concurrent
                        try {
                            handle.execute();
                        } finally {
                            this.currentTaskHandle = null;
                        }
                    }
                } else if (compareAndSetState(state, WORKER_IDLE)) {//4: park work thread
                    if (workerKeepaliveTimed)
                        LockSupport.parkNanos(workerKeepAliveTime);
                    else
                        LockSupport.park();

                    compareAndSetState(WORKER_IDLE, WORKER_TERMINATED);
                }

                Thread.interrupted();//clean interruption state
            } while (true);

            //remove worker from pool by self
            workerCount.decrementAndGet();
            workerQueue.remove(this);
        }
    }

    private class PoolScheduledTaskPeekThread extends Thread {
        PoolScheduledTaskPeekThread() {
            this.setName(poolName + "-ScheduledPeek");
            this.setDaemon(true);
        }

        public void run() {
            while (true) {
                int poolCurState = poolState;
                if (poolCurState == POOL_RUNNING) {
                    //1: poll expired task
                    Object polledObject = scheduledDelayedQueue.pollExpired();
                    //2: if polled object is expired schedule task
                    if (polledObject instanceof ScheduledTaskHandle) {
                        ScheduledTaskHandle taskHandle = (ScheduledTaskHandle) polledObject;
                        if (taskHandle.state == TASK_WAITING)
                            pushToExecutionQueue(taskHandle);//push it to execution queue
                        else
                            taskHoldingCount.decrementAndGet();//task has cancelled,so remove it
                    } else {//3: the polled object is time,then park
                        Long time = (Long) polledObject;
                        if (time > 0) {
                            LockSupport.parkNanos(time);
                        } else {
                            LockSupport.park();
                        }
                    }
                }

                //4: pool state check,if in clearing,then park peek thread
                if (poolCurState == POOL_CLEARING) LockSupport.park();
                if (poolCurState > POOL_CLEARING) break;
            }
        }
    }
}
