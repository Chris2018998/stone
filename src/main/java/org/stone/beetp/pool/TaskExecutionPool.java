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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beetp.TaskStates.TASK_CANCELLED;
import static org.stone.beetp.TaskStates.TASK_WAITING;
import static org.stone.beetp.pool.TaskPoolConstants.*;

/**
 * Task Pool Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskExecutionPool implements TaskPool {
    private static final AtomicIntegerFieldUpdater<TaskExecutionPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(TaskExecutionPool.class, "poolState");

    //1:fields about pool
    private String poolName;
    private volatile int poolState;

    //2: fields about tasks
    private int maxTaskSize;
    private long completedCount;//completion count of tasks,worker threads update this value after executed tasks end
    // update in <method>removeTaskWorker</method>
    private AtomicInteger taskCount;//(count of once tasks + count of scheduled tasks + root count of joined tasks)

    //2: fields about worker threads
    private int maxWorkerSize;
    private boolean workInDaemon;
    private long idleTimeoutNanos;
    private boolean idleTimeoutValid;
    private ReentrantLock workerArrayLock;
    private volatile TaskWorkThread[] workerArray;
    private ConcurrentLinkedQueue<BaseHandle> taskQueue;

    //4: fields about task execution
    private TaskPoolMonitorVo monitorVo;
    private ScheduledTaskQueue scheduledDelayedQueue;
    private PoolScheduledTaskPollThread scheduledPeekThread;//wait at first task of scheduled queue util first task timeout,then poll it from queue
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
        //step1: assign values to some local variables
        this.poolName = config.getPoolName();
        this.maxTaskSize = config.getMaxTaskSize();
        this.maxWorkerSize = config.getMaxWorkerSize();
        this.workInDaemon = config.isWorkerInDaemon();
        this.idleTimeoutNanos = MILLISECONDS.toNanos(config.getWorkerKeepAliveTime());
        this.idleTimeoutValid = this.idleTimeoutNanos > 0L;

        //step2: create task queue and pool lock
        if (workerArray == null) {
            this.workerArrayLock = new ReentrantLock();
            this.taskQueue = new ConcurrentLinkedQueue<>();
            this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

            //step3: atomic fields of pool monitor
            this.taskCount = new AtomicInteger();
            this.monitorVo = new TaskPoolMonitorVo();
        }

        //step4: create worker threads on pool initialization
        String workerName = poolName + "-task worker";
        int workerInitSize = config.getInitWorkerSize();
        this.workerArray = new TaskWorkThread[workerInitSize];
        for (int i = 0; i < workerInitSize; i++) {
            TaskWorkThread worker = new TaskWorkThread(WORKER_WORKING, this);
            worker.setDaemon(workInDaemon);
            worker.setName(workerName);
            workerArray[i] = worker;
        }
        //delay to start to avoid to scan null workers
        for (int i = 0; i < workerInitSize; i++) {
            workerArray[i].start();
        }

        //step5: create a queue to store scheduled tasks and a thread to poll expired tasks from scheduled tasks queue and pull to execution queue
        if (scheduledPeekThread == null) {
            this.scheduledDelayedQueue = new ScheduledTaskQueue(0);
            this.scheduledPeekThread = new PoolScheduledTaskPollThread();
            this.scheduledPeekThread.start();
        }
    }

    //***************************************************************************************************************//
    //                                       2: task submission(6)                                                   //
    //***************************************************************************************************************//
    public TaskHandle submit(Task task) throws TaskException {
        return submit(task, (TaskCallback) null);
    }

    public TaskHandle submit(Task task, TaskCallback callback) throws TaskException {
        //1: check task
        if (task == null) throw new TaskException("Task can't be null");
        //2: check pool state and task capacity
        this.checkPool();

        //3: create task handle
        BaseHandle handle = new BaseHandle(task, callback, this);
        //4: push task to execution queue
        this.pushToExecutionQueue(handle, this.taskQueue);
        //5: return handle
        return handle;
    }

    public TaskHandle submit(Task task, TaskJoinOperator operator) throws TaskException {
        return submit(task, operator, null);
    }

    public TaskHandle submit(Task task, TaskJoinOperator operator, TaskCallback callback) throws TaskException {
        //1: check task
        if (task == null) throw new TaskException("Task can't be null");
        if (operator == null) throw new TaskException("Task join operator can't be null");
        //2: check pool state and task capacity
        this.checkPool();

        //3: create join task handle(root)
        BaseHandle handle = new JoinTaskHandle(task, operator, callback, this);
        //4: push task to execution queue
        this.pushToExecutionQueue(handle, this.taskQueue);
        //5: return handle
        return handle;
    }

    public TaskHandle submit(TreeTask task) throws TaskException {
        return submit(task, null);
    }

    public TaskHandle submit(TreeTask task, TaskCallback callback) throws TaskException {
        //1: check task
        if (task == null) throw new TaskException("Task can't be null");
        //2: check pool state and task capacity
        this.checkPool();

        //3: create tree task handle(root)
        TreeTaskHandle handle = new TreeTaskHandle(task, callback, this);
        //4: push task to execution queue
        this.pushToExecutionQueue(handle, this.taskQueue);
        //5: return handle
        return handle;
    }

    //***************************************************************************************************************//
    //                                    3: Scheduled task submit(6)                                                //
    //***************************************************************************************************************//
    public TaskScheduledHandle schedule(Task task, long delay, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, delay, 0, false, null, 1);
    }

    public TaskScheduledHandle scheduleAtFixedRate(Task task, long initialDelay, long period, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, null, 2);
    }

    public TaskScheduledHandle scheduleWithFixedDelay(Task task, long initialDelay, long delay, TimeUnit unit) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, null, 3);
    }

    public TaskScheduledHandle schedule(Task task, long delay, TimeUnit unit, TaskCallback callback) throws TaskException {
        return addScheduleTask(task, unit, delay, 0, false, callback, 1);
    }

    public TaskScheduledHandle scheduleAtFixedRate(Task task, long initialDelay, long period, TimeUnit unit, TaskCallback callback) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, period, false, callback, 2);
    }

    public TaskScheduledHandle scheduleWithFixedDelay(Task task, long initialDelay, long delay, TimeUnit unit, TaskCallback callback) throws TaskException {
        return addScheduleTask(task, unit, initialDelay, delay, true, callback, 3);
    }

    //***************************************************************************************************************//
    //                                  4: task check and task offer(4)                                              //
    //***************************************************************************************************************//
    private void checkPool() throws TaskException {
        //1: pool state check
        if (this.poolState != POOL_RUNNING)
            throw new TaskRejectedException("Pool has been closed or in clearing");

        //2: task capacity full check
        do {
            int currentCount = taskCount.get();
            if (currentCount >= maxTaskSize) throw new TaskRejectedException("Capacity of tasks has reached max size");
            if (taskCount.compareAndSet(currentCount, currentCount + 1)) return;
        } while (true);
    }

    //push task to execution queue(**scheduled peek thread calls this method to push task**)
    void pushToExecutionQueue(BaseHandle taskHandle, Queue<BaseHandle> taskQueue) {
        //1:try to wakeup a idle work thread to process this task
        TaskWorkThread[] workers = this.workerArray;
        for (TaskWorkThread worker : workers) {
            if (worker.compareAndSetState(WORKER_IDLE, taskHandle)) {
                LockSupport.unpark(worker);
                return;
            }
        }

        //2:try to create a new worker
        if (this.workerArray.length >= this.maxWorkerSize || this.createTaskWorker(taskHandle) == null)
            taskQueue.offer(taskHandle);
    }

    void wakeupSchedulePeekThread() {
        LockSupport.unpark(scheduledPeekThread);
    }

    private TaskScheduledHandle addScheduleTask(Task task, TimeUnit unit, long initialDelay, long intervalTime, boolean fixedDelay, TaskCallback callback, int scheduledType) throws TaskException {
        //1: check task
        if (task == null) throw new TaskException("Task can't be null");
        if (unit == null) throw new TaskException("Task time unit can't be null");
        if (initialDelay < 0)
            throw new TaskException(scheduledType == 1 ? "Delay" : "Initial delay" + " time can't be less than zero");
        if (intervalTime <= 0 && scheduledType != 1)
            throw new TaskException(scheduledType == 2 ? "Period" : "Delay" + " time must be greater than zero");

        //2: check pool state and task capacity
        this.checkPool();

        //3: create task handle
        long intervalNanos = unit.toNanos(intervalTime);
        long firstRunNanos = unit.toNanos(initialDelay) + System.nanoTime();
        ScheduledTaskHandle handle = new ScheduledTaskHandle(task, callback, firstRunNanos, intervalNanos, fixedDelay, this);

        //4: add task handle to time sortable array,and gets its index in array
        int index = scheduledDelayedQueue.add(handle);

        //re-check pool state,if not in running,then try to cancel task
        if (this.poolState != POOL_RUNNING) {
            if (handle.setAsCancelled()) {
                if (scheduledDelayedQueue.remove(handle) >= 0) taskCount.decrementAndGet();
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
        } catch (TaskServiceConfigException e) {
            return false;
        }
    }

    public boolean clear(boolean mayInterruptIfRunning, TaskServiceConfig config) throws TaskServiceConfigException {
        TaskServiceConfig checkedConfig = null;
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

    private TaskPoolTerminatedVo removeAll(boolean mayInterruptIfRunning) {
        List<Task> onceTaskList = new LinkedList<>();
        List<Task> scheduledTaskList = new LinkedList<>();
        List<Task> joinTaskList = new LinkedList<>();
        List<TreeTask> treeTaskList = new LinkedList<>();

        //1: remove scheduled tasks
        for (ScheduledTaskHandle handle : scheduledDelayedQueue.clearAll()) {
            if (handle.setAsCancelled()) {//collect cancelled tasks by execution
                scheduledTaskList.add(handle.task);
                handle.setResult(TASK_CANCELLED, null);
            }
        }

        //2: remove tasks in execution queue(once tasks + join tasks)
        BaseHandle handle;
        while ((handle = taskQueue.poll()) != null) {
            if (handle.setAsCancelled()) {//collect cancelled tasks by execution
                handle.setResult(TASK_CANCELLED, null);
                if (handle instanceof TreeTaskHandle) {
                    treeTaskList.add(((TreeTaskHandle) handle).getTreeTask());
                } else if (handle instanceof JoinTaskHandle) {
                    joinTaskList.add(handle.task);
                } else if (!(handle instanceof ScheduledTaskHandle)) {
                    onceTaskList.add(handle.task);
                }
            }
        }

        //3: remove work threads
        for (TaskWorkThread workerThread : workerArray) {
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
        this.completedCount = 0;
        this.taskCount.set(0);
        this.workerArray = new TaskWorkThread[0];
        return new TaskPoolTerminatedVo(
                onceTaskList,
                scheduledTaskList,
                joinTaskList,
                treeTaskList);
    }

    //remove from array or queue(method called inside handle)
    void removeCancelledTask(BaseHandle handle) {
        if (handle instanceof ScheduledTaskHandle) {
            int taskIndex = scheduledDelayedQueue.remove((ScheduledTaskHandle) handle);
            if (taskIndex >= 0) taskCount.decrementAndGet();//task removed successfully by call thread
            if (taskIndex == 0) wakeupSchedulePeekThread();
        } else if (taskQueue.remove(handle) && handle.isRoot) {
            taskCount.decrementAndGet();
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

    public TaskPoolTerminatedVo terminate(boolean mayInterruptIfRunning) throws TaskPoolException {
        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_TERMINATING)) {
            TaskPoolTerminatedVo info = this.removeAll(mayInterruptIfRunning);

            this.poolState = POOL_TERMINATED;
            for (Thread thread : poolTerminateWaitQueue)
                LockSupport.unpark(thread);
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
    ConcurrentLinkedQueue<BaseHandle> getTaskQueue() {
        return taskQueue;
    }

    TaskWorkThread[] getWorkerArray() {
        return this.workerArray;
    }

    boolean isIdleTimeoutValid() {
        return this.idleTimeoutValid;
    }

    long getIdleTimeoutNanos() {
        return this.idleTimeoutNanos;
    }

    AtomicInteger getTaskCount() {
        return this.taskCount;
    }

    ScheduledTaskQueue getScheduledDelayedQueue() {
        return scheduledDelayedQueue;
    }

    public TaskPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setPoolState(this.poolState);
        monitorVo.setWorkerCount(workerArray.length);
        monitorVo.setTaskHoldingCount(taskCount.get());
        int runningCount = 0;
        List<BaseHandle> runningTasks = new ArrayList<>(10);
        for (TaskWorkThread worker : workerArray) {
            completedCount += worker.completedCount;
            BaseHandle curTaskHandle = worker.curTaskHandle;
            if (curTaskHandle != null) {
                BaseHandle rootHandle = null;
                if (curTaskHandle.isRoot) {
                    rootHandle = curTaskHandle;
                } else if (curTaskHandle instanceof JoinTaskHandle) {
                    rootHandle = ((JoinTaskHandle) curTaskHandle).root;
                } else if (curTaskHandle instanceof TreeTaskHandle) {
                    rootHandle = ((TreeTaskHandle) curTaskHandle).root;
                }

                if (!runningTasks.contains(rootHandle)) {
                    runningTasks.add(rootHandle);
                    runningCount++;
                }
            }
        }

        monitorVo.setTaskRunningCount(runningCount);
        monitorVo.setTaskCompletedCount(completedCount);
        return monitorVo;
    }

    //***************************************************************************************************************//
    //                                  8: worker thread creation or remove                                          //
    //***************************************************************************************************************//
    private TaskWorkThread createTaskWorker(Object taskHandle) {
        this.workerArrayLock.lock();
        try {
            int l = this.workerArray.length;
            if (l < this.maxWorkerSize) {
                TaskWorkThread worker = new TaskWorkThread(taskHandle, this);
                worker.setDaemon(workInDaemon);
                worker.setName(poolName + "-task worker");
                worker.start();

                TaskWorkThread[] arrayNew = new TaskWorkThread[l + 1];
                System.arraycopy(this.workerArray, 0, arrayNew, 0, l);
                arrayNew[l] = worker;
                this.workerArray = arrayNew;
                return worker;
            }
            return null;
        } finally {
            workerArrayLock.unlock();
        }
    }

    void removeTaskWorker(TaskWorkThread worker) {
        this.workerArrayLock.lock();
        try {
            //add completed count of worker to pool
            this.completedCount += worker.completedCount;
            for (int l = this.workerArray.length, i = l - 1; i >= 0; i--) {
                if (this.workerArray[i] == worker) {
                    TaskWorkThread[] arrayNew = new TaskWorkThread[l - 1];
                    System.arraycopy(this.workerArray, 0, arrayNew, 0, i);//copy pre
                    int m = l - i - 1;
                    if (m > 0) System.arraycopy(this.workerArray, i + 1, arrayNew, i, m);//copy after
                    this.workerArray = arrayNew;
                    break;
                }
            }
        } finally {
            workerArrayLock.unlock();
        }
    }

    //***************************************************************************************************************//
    //                                 9: Pool worker class and scheduled class (2)                                  //
    //***************************************************************************************************************//
    private class PoolScheduledTaskPollThread extends Thread {
        PoolScheduledTaskPollThread() {
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
                            pushToExecutionQueue(taskHandle, taskQueue);//push it to execution queue
                        else
                            taskCount.decrementAndGet();//task has cancelled,so remove it
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
