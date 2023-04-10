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
import org.stone.util.SortedArray;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
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
    private int maxQueueSize;
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

    //peek schedule tasks from array then push to execute queue
    private PoolScheduleAssignThread schedulerThread;
    private SortedArray<TaskScheduleHandle> scheduledTaskArray;

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
        this.maxQueueSize = checkedConfig.getQueueMaxSize();
        this.maxWorkerSize = checkedConfig.getMaxWorkerSize();
        this.workerInDaemon = checkedConfig.isWorkInDaemon();
        this.workerMaxAliveTime = MILLISECONDS.toNanos(checkedConfig.getWorkerKeepAliveTime());
        this.monitorVo = new TaskPoolMonitorVo();
        this.poolInterceptor = checkedConfig.getPoolInterceptor();

        //step4: create workers by configured initialized size
        int workerInitSize = config.getInitWorkerSize();
        this.workerCountInQueue.set(workerInitSize);
        for (int i = 0; i < workerInitSize; i++) {
            PoolWorkerThread worker = new PoolWorkerThread(this, null);
            workerQueue.offer(worker);
            worker.start();
        }

        //step5: create task queue full reject policy
        switch (checkedConfig.getQueueFullPolicyCode()) {
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

        //step6: create scheduled task array
        this.scheduledTaskArray = new SortedArray<>(TaskScheduleHandle.class, 0,
                new Comparator<TaskScheduleHandle>() {
                    public int compare(TaskScheduleHandle handle1, TaskScheduleHandle handle2) {
                        long compareV = handle1.getExecuteTimePoint() - handle2.getExecuteTimePoint();
                        if (compareV > 0) return 1;
                        if (compareV == 0) return 0;
                        return -1;
                    }
                });
        this.schedulerThread = new PoolScheduleAssignThread(this);
        this.schedulerThread.start();

        //step7: set pool state to be ready for submitting tasks
        this.poolState = POOL_READY;

        //step8: execute pool Interceptor(if exists)
        if (poolInterceptor != null) {
            try {
                poolInterceptor.afterStartup();
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    //***************************************************************************************************************//
    //                2: execution task methods(2)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void removeExecuteTask(TaskHandleImpl handle) {
        taskQueue.remove(handle);
    }

    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException, BeeTaskPoolException {
        //1: check pool state
        if (task == null) throw new TaskExecutionException("Task can't be null");
        if (this.poolState != POOL_READY)
            throw new PoolSubmitRejectedException("Access forbidden,task pool was closed or in clearing");

        //2: execute reject policy on task queue full
        boolean offerInd = true;
        if (taskCountInQueue.get() == maxQueueSize) offerInd = rejectPolicy.rejectTask(task, this);

        //3: create task handle and offer it to queue by indicator
        TaskHandleImpl taskHandle = new TaskHandleImpl(task, offerInd ? TASK_NEW : TASK_CANCELLED, this);
        if (offerInd) offerToTaskQueue(taskHandle);

        return taskHandle;
    }

    //offer task to queue and try to wakeup a work thread
    private void offerToTaskQueue(TaskHandleImpl taskHandle) {
        taskQueue.offer(taskHandle);
        //1: try to wakeup one when exists idle work thread(this different with JDK)
        for (PoolWorkerThread workerThread : workerQueue) {
            if (workerThread.compareAndSetState(WORKER_IDLE, taskHandle)) {
                LockSupport.unpark(workerThread);
                return;
            }
        }

        //2: try to create a new work thread to work
        do {
            int currentCount = this.workerCountInQueue.get();
            if (currentCount >= this.maxWorkerSize) return;
            if (workerCountInQueue.compareAndSet(currentCount, currentCount + 1)) {
                PoolWorkerThread worker = new PoolWorkerThread(this, taskHandle);
                workerQueue.offer(worker);
                worker.start();
                return;
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                2: schedule task methods(3)                                                                    //                                                                                  //
    //***************************************************************************************************************//
    void removeScheduleTask(TaskScheduleHandle handle) {
        taskQueue.remove(handle);
    }

    public BeeTaskHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException {
        if (task == null || unit == null) throw new NullPointerException();
        if (delay <= 0) throw new IllegalArgumentException();

        TaskScheduleHandle handle = new TaskScheduleHandle(task, 0, this);
        int pos = scheduledTaskArray.add(handle);
        if (pos == 0) {//wakeup schedule thread to work

        }
        return handle;
    }

    public BeeTaskHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException {
        if (task == null || unit == null) throw new NullPointerException();
        if (period <= 0) throw new IllegalArgumentException();

        TaskScheduleHandle handle = new TaskScheduleHandle(task, 0, this);
        int pos = scheduledTaskArray.add(handle);
        if (pos == 0) {

        }
        return handle;
    }

    public BeeTaskHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException {
        if (task == null || unit == null) throw new NullPointerException();
        if (period <= 0) throw new IllegalArgumentException();

        TaskScheduleHandle handle = new TaskScheduleHandle(task, 0, this);
        int pos = scheduledTaskArray.add(handle);
        if (pos == 0) {

        }
        return handle;
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
            if (mayInterruptIfRunning) {
                while ((worker = workerQueue.poll()) != null) {
                    worker.setState(WORKER_TERMINATED);
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

            TaskHandleImpl taskHandle;
            List<BeeTask> queueTaskList = new LinkedList<>();
            while ((taskHandle = taskQueue.poll()) != null) {
                queueTaskList.add(taskHandle.getTask());
                taskHandle.setState(TASK_CANCELLED);
                taskHandle.wakeupWaiters();
            }

            this.poolState = POOL_TERMINATED;
            this.wakeupTerminationWaiters();
            if (poolInterceptor != null) {
                try {
                    poolInterceptor.afterTerminated();
                } catch (Throwable ee) {
                    //do nothing
                }
            }

//            try {
//                Runtime.getRuntime().removeShutdownHook(this.exitHook);
//            } catch (Throwable e) {
//                //do nothing
//            }
            return queueTaskList;
        } else {
            throw new BeeTaskPoolException("Termination forbidden,pool has been in terminating or afterTerminated");
        }
    }

    public boolean clear(boolean mayInterruptIfRunning) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            TaskHandleImpl taskHandle;
            while ((taskHandle = taskQueue.poll()) != null) {
                taskHandle.setState(TASK_CANCELLED);
                taskHandle.wakeupWaiters();
            }

            PoolWorkerThread worker;
            if (mayInterruptIfRunning) {
                while ((worker = workerQueue.poll()) != null) {
                    worker.setState(WORKER_TERMINATED);
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
        if (timeout < 0) throw new IllegalArgumentException("Time out value must be greater than zero");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");

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
        workerCountInQueue.decrementAndGet();
        if (!handle.compareAndSetState(TASK_NEW, TASK_RUNNING)) return;

        try {
            handle.setWorkThread(Thread.currentThread());
            runningTaskCount.incrementAndGet();
            BeeTask task = handle.getTask();
            //1: execute pool interceptor
            if (poolInterceptor != null) {
                try {
                    poolInterceptor.beforeCall(task, handle);
                } catch (Throwable e) {
                    //do nothing
                }
            }
            //2: execute task aspect
            BeeTaskAspect aspect = task.getAspect();
            if (aspect != null) {
                try {
                    aspect.beforeCall(handle);
                } catch (Throwable e) {
                    //do nothing
                }
            }
            //3: execute task
            try {
                Object result = task.call();
                handle.setDone(TASK_CALL_RESULT, result);

                if (poolInterceptor != null) {
                    try {
                        poolInterceptor.afterCall(task, result, handle);
                    } catch (Throwable e) {
                        //do nothing
                    }
                }
                if (aspect != null) {
                    try {
                        aspect.afterCall(result, handle);
                    } catch (Throwable e) {
                        //do nothing
                    }
                }
            } catch (Throwable e) {
                handle.setDone(TASK_EXCEPTIONAL, new TaskExecutionException(e));
                if (poolInterceptor != null) {
                    try {
                        poolInterceptor.afterThrowing(task, e, handle);
                    } catch (Throwable ee) {
                        //do nothing
                    }
                }
                if (aspect != null) {
                    try {
                        aspect.afterThrowing(e, handle);
                    } catch (Throwable ee) {
                        //do nothing
                    }
                }
            }
        } finally {
            handle.setWorkThread(null);
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
                oldTask.setState(TASK_CANCELLED);
                oldTask.wakeupWaiters();
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
        private final TaskExecutionPool pool;
        private final boolean keepaliveTimed;
        private final long workerKeepAliveTime;
        private final AtomicReference workState;
        private final ConcurrentLinkedQueue<TaskHandleImpl> taskQueue;

        PoolWorkerThread(TaskExecutionPool pool, Object state) {
            this.pool = pool;
            this.setDaemon(pool.workerInDaemon);
            this.taskQueue = pool.taskQueue;
            this.workerKeepAliveTime = pool.workerMaxAliveTime;
            this.keepaliveTimed = workerKeepAliveTime > 0;
            this.setName(pool.poolName + "-worker thread-" + Index.getAndIncrement());
            this.workState = new AtomicReference(state);
        }

        void setState(Object update) {
            workState.set(update);
        }

        boolean compareAndSetState(Object expect, Object update) {
            return workState.compareAndSet(expect, update);
        }

        public void run() {
//            TaskHandleImpl task = null;
//            do {
//                Object state = workState.get();
//                if (state == WORKER_TERMINATED || pool.poolState >= POOL_TERMINATING)
//                    break;
//
//                if (state instanceof TaskHandleImpl)
//                    task = (TaskHandleImpl) state;
//
//                if (task == null) task = taskQueue.poll();
//                if (task != null) pool.executeTask(task);
//
//
//            } else if (compareAndSetState(WORKER_RUNNING, WORKER_IDLE)) {
//                if (keepaliveTimed)
//                    LockSupport.parkNanos(workerKeepAliveTime);
//                else
//                    LockSupport.park();
//
//                //keep alive timeout,then try to exit
//                compareAndSetState(WORKER_IDLE, WORKER_TERMINATED);
//            }
//
//            Thread.interrupted();//clear interrupted state flag(may be interrupted in task call or worker park)
//        } while(true);
//
//
//                pool.workerCountInQueue.decrementAndGet()
//                pool.workerQueue.remove(this)
        }
    }

    private static class PoolScheduleAssignThread extends Thread {
        private final TaskExecutionPool pool;

        PoolScheduleAssignThread(TaskExecutionPool pool) {
            this.pool = pool;
            this.setName(pool.poolName + "-ScheduleAssignThread");
            this.setDaemon(true);
        }

        public void run() {
            while (pool.poolState >= POOL_TERMINATING) {


            }
        }
    }
}


//    private static class PoolExitJvmHook extends Thread {
//        private final TaskExecutionPool pool;
//
//        PoolExitJvmHook(TaskExecutionPool pool) {
//            this.pool = pool;
//        }
//
//        public void run() {
//            try {
//                pool.terminate(pool.interruptWorkerOnClear);
//            } catch (Throwable e) {
//                //do nothing
//            }
//        }
//    }
