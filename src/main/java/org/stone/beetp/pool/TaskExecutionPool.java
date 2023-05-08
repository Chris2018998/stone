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
    private int maxQueueSize;
    private int maxWorkerSize;
    private boolean workerInDaemon;
    private long workerKeepAliveTime;

    private TaskPoolMonitorVo monitorVo;
    private AtomicInteger taskCountInQueue;
    private AtomicInteger workerCountInQueue;
    private AtomicInteger runningTaskCount;
    private AtomicInteger completedTaskCount;

    //store sortable scheduled tasks
    private SortedArray<TaskScheduledHandle> scheduledArray;
    //peek scheduled tasks,then push to execution queue
    private PoolScheduledTaskPeekThread scheduledTaskPeekThread;
    private ConcurrentLinkedQueue<PoolWorkerThread> workerQueue;
    private ConcurrentLinkedQueue<TaskExecuteHandle> executionQueue;
    //pool terminated wait queue
    private ConcurrentLinkedQueue<Thread> poolTerminateWaitQueue;

    //***************************************************************************************************************//
    //                1: pool initialization(1)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeTaskServiceConfig config) throws BeeTaskPoolException, BeeTaskServiceConfigException {
        //step1: pool config check
        if (config == null) throw new PoolInitializedException("Pool configuration can't be null");
        BeeTaskServiceConfig checkedConfig = config.check();

        //step2: simple attribute set
        this.poolName = checkedConfig.getPoolName();
        this.maxQueueSize = checkedConfig.getTaskMaxSize();
        this.maxWorkerSize = checkedConfig.getMaxWorkerSize();
        this.workerInDaemon = checkedConfig.isWorkInDaemon();
        this.workerKeepAliveTime = MILLISECONDS.toNanos(checkedConfig.getWorkerKeepAliveTime());

        //step3: creation of queues
        this.workerQueue = new ConcurrentLinkedQueue<>();
        this.executionQueue = new ConcurrentLinkedQueue<>();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        //step4: creation of monitor fields
        this.monitorVo = new TaskPoolMonitorVo();
        this.taskCountInQueue = new AtomicInteger();
        this.workerCountInQueue = new AtomicInteger();
        this.runningTaskCount = new AtomicInteger();
        this.completedTaskCount = new AtomicInteger();

        //step5: prepare to startup some worker threads by config
        int workerInitSize = config.getInitWorkerSize();
        this.workerCountInQueue.set(workerInitSize);
        for (int i = 0; i < workerInitSize; i++) {
            PoolWorkerThread worker = new PoolWorkerThread(this, null);
            workerQueue.offer(worker);
            worker.start();
        }

        //step6: create scheduled task array and peek thread
        this.scheduledArray = new SortedArray<>(TaskScheduledHandle.class, 0,
                new Comparator<TaskScheduledHandle>() {
                    public int compare(TaskScheduledHandle handle1, TaskScheduledHandle handle2) {
                        long compareV = handle1.getNextExecutionTime() - handle2.getNextExecutionTime();
                        if (compareV > 0) return 1;
                        if (compareV == 0) return 0;
                        return -1;
                    }
                });
        this.scheduledTaskPeekThread = new PoolScheduledTaskPeekThread(this);
        this.scheduledTaskPeekThread.start();

        //step7: set pool state to be ready for submitting tasks
        this.poolState = POOL_READY;
    }

    //***************************************************************************************************************//
    //                2: task submit(3)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException {
        return this.submit(new BeeTaskConfig(task));
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskCallback listener) throws BeeTaskException {
        return this.submit(new BeeTaskConfig(task, listener));
    }

    public BeeTaskHandle submit(BeeTaskConfig taskConfig) throws BeeTaskException {
        //1:pool check
        if (this.poolState != POOL_READY)
            throw new TaskRejectedException("Access forbidden,task pool was closed or in clearing");
        //2:task config check
        if (taskConfig == null) throw new TaskExecutionException("Task can't be null");
        taskConfig.check();

        //3:offer test(check failed then throws rejection exception)
        if (!offerTest()) throw new TaskRejectedException("Pool was full,task rejected");

        //4:create task handle by config
        TaskExecuteHandle handle = createTaskHandle(taskConfig);

        //5:offer to queue(scheduled type task will be added to scheduledArray)
        if (handle instanceof TaskScheduledHandle) {
            if (scheduledArray.add((TaskScheduledHandle) handle) == 0) {
                /**
                 * if new task is on first
                 * @todo wakeup thread to peek new
                 */
            }
        } else {
            pushToExecutionQueue(handle);
        }
        //5.1:need recheck pool state is valid,if not,then cancel,remove from pool and throw exception?

        //6:return handle to outside caller
        return handle;
    }

    //***************************************************************************************************************//
    //                3: task offer and remove(3)                                                                    //                                                                                  //
    //***************************************************************************************************************//
    //true,pool not full;false,pool full
    private boolean offerTest() {
        do {
            int currentSize = taskCountInQueue.get();
            if (currentSize >= maxQueueSize) return false;//pool is full
            if (taskCountInQueue.compareAndSet(currentSize, currentSize + 1))
                return true;
        } while (true);
    }

    //push task to execution queue(**scheduled peek thread call this method to push task**)
    private void pushToExecutionQueue(TaskExecuteHandle taskHandle) {
        //1:offer to task queue
        executionQueue.offer(taskHandle);

        //2:try to wakeup a idle worker to handle the task
        for (PoolWorkerThread workerThread : workerQueue) {
            if (workerThread.compareAndSetState(WORKER_IDLE, taskHandle)) {
                LockSupport.unpark(workerThread);
                return;
            }
        }

        //3:if not exists an idle worker,then try to create a new worker to handle it
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

    //create task handle
    private TaskExecuteHandle createTaskHandle(BeeTaskConfig taskConfig) {
        if (taskConfig.getInitDelayTime() != 0 || taskConfig.getPeriodDelayTime() != 0) {
            return new TaskExecuteHandle(taskConfig.getTask(),
                    TASK_WAITING, taskConfig.getCallback(), this);
        } else {
            TaskScheduledHandle handle = new TaskScheduledHandle(taskConfig.getTask(),
                    TASK_WAITING, taskConfig.getCallback(), this);

            //@todo time caluate
            long delayNanoseconds = taskConfig.getTimeUnit().toNanos(taskConfig.getPeriodDelayTime());
            long firstExecutionTime = 0;
            handle.setScheduledTime(firstExecutionTime, delayNanoseconds, taskConfig.getFixedRateDelay());
            return handle;
        }
    }

    //remove from array or queue
    void removeCancelledTask(TaskExecuteHandle handle) {
        if (handle instanceof TaskScheduledHandle) {
            if (scheduledArray.remove((TaskScheduledHandle) handle) > -1) {
                taskCountInQueue.decrementAndGet();
            }
        } else if (executionQueue.remove(handle)) {
            taskCountInQueue.decrementAndGet();
        }
    }

    //***************************************************************************************************************//
    //                2: schedule task methods(3)                                                                    //                                                                                  //
    //***************************************************************************************************************//
    void removeScheduleTask(TaskScheduledHandle handle) {
        executionQueue.remove(handle);
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

            TaskExecuteHandle taskHandle;
            List<BeeTask> queueTaskList = new LinkedList<>();
            while ((taskHandle = executionQueue.poll()) != null) {
                queueTaskList.add(taskHandle.getTask());
                taskHandle.setCurState(TASK_CANCELLED);
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
            TaskExecuteHandle taskHandle;
            while ((taskHandle = executionQueue.poll()) != null) {
                taskHandle.setCurState(TASK_CANCELLED);
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
    private void executeTask(TaskExecuteHandle handle) {
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
            BeeTaskCallback aspect = task.getAspect();
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
                        aspect.onReturn(result, handle);
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
                        aspect.onCatch(e, handle);
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
    private static class PoolWorkerThread extends Thread {
        private static final AtomicInteger Index = new AtomicInteger(1);
        private final TaskExecutionPool pool;
        private final boolean keepaliveTimed;
        private final long workerKeepAliveTime;
        private final AtomicReference workState;
        private final ConcurrentLinkedQueue<TaskExecuteHandle> taskQueue;

        PoolWorkerThread(TaskExecutionPool pool, Object state) {
            this.pool = pool;
            this.setDaemon(pool.workerInDaemon);
            this.taskQueue = pool.executionQueue;
            this.workerKeepAliveTime = pool.workerKeepAliveTime;
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
            do {
                //1: read state from work
                Object state = workState.get();
                if (state == WORKER_TERMINATED || pool.poolState >= POOL_TERMINATING)
                    break;

                //2: get task from state or poll from queue
                TaskExecuteHandle task;
                if (state instanceof TaskExecuteHandle) {
                    task = (TaskExecuteHandle) state;
                } else if ((task = taskQueue.poll()) != null) {
                    pool.workerCountInQueue.decrementAndGet();
                }

                //3: execute task
                if (task != null && task.setAsRunning()) {
                    pool.executeTask(task);
                } else if (compareAndSetState(state, WORKER_IDLE)) {//4: park work thread
                    if (keepaliveTimed)
                        LockSupport.parkNanos(workerKeepAliveTime);
                    else
                        LockSupport.park();

                    compareAndSetState(WORKER_IDLE, WORKER_TERMINATED);
                }

                //try to clear interrupted flag state
                Thread.interrupted();
            } while (true);

            pool.workerCountInQueue.decrementAndGet();
            pool.workerQueue.remove(this);
        }
    }

    private static class PoolScheduledTaskPeekThread extends Thread {
        private final TaskExecutionPool pool;

        PoolScheduledTaskPeekThread(TaskExecutionPool pool) {
            this.pool = pool;
            this.setName(pool.poolName + "-ScheduleTaskPeek");
            this.setDaemon(true);
        }

        public void run() {
            while (pool.poolState >= POOL_TERMINATING) {


            }
        }
    }
}

//    private interface TaskRejectPolicy {
//        //true:rejected;false:continue;
//        boolean rejectTask(BeeTask task, TaskExecutionPool pool) throws BeeTaskException, BeeTaskPoolException;
//    }
//
//    private static class TaskAbortPolicy implements TaskRejectPolicy {
//        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) throws BeeTaskPoolException {
//            throw new PoolSubmitRejectedException("");
//        }
//    }
//
//    private static class TaskDiscardPolicy implements TaskRejectPolicy {
//        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) {
//            return true;
//        }
//    }
//
//    private static class TaskRemoveOldestPolicy implements TaskRejectPolicy {
//        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) {
//            TaskExecuteHandle oldTask = pool.executionQueue.poll();
//            if (oldTask != null) {
//                pool.taskCountInQueue.decrementAndGet();
//                oldTask.setCurState(TASK_CANCELLED);
//                oldTask.wakeupWaiters();
//            }
//            return false;
//        }
//    }
//
//    private static class TaskCallerRunsPolicy implements TaskRejectPolicy {
//        public boolean rejectTask(BeeTask task, TaskExecutionPool pool) throws BeeTaskException {
//            try {
//                task.call();
//                return true;
//            } catch (Throwable e) {
//                throw new BeeTaskException(e);
//            }
//        }
//    }

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
