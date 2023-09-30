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

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
    private AtomicInteger taskHoldingCount;//(once count + scheduled count)
    private AtomicLong taskRunningCount;
    private AtomicLong taskCompletedCount;
    private TaskPoolMonitorVo monitorVo;
    private AtomicInteger workerNameIndex;
    private ConcurrentLinkedQueue<PoolWorkerThread> workerQueue;
    private ConcurrentLinkedQueue<BaseHandle> executionQueue;
    private ScheduledTaskQueue scheduledQueue;
    private PoolScheduledTaskPeekThread scheduledTaskPeekThread;//poll from scheduledQueue and push to executionQueue

    private OnceExecFactory onceExecFactory;
    private JoinExecFactory joinExecFactory;
    private ScheduledExecFactory scheduledExecFactory;
    private ConcurrentLinkedQueue<Thread> poolTerminateWaitQueue;

    //***************************************************************************************************************//
    //                                          1: pool initialization(1)                                            //
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
        this.workerNameIndex = new AtomicInteger();
        this.workerQueue = new ConcurrentLinkedQueue<>();
        this.executionQueue = new ConcurrentLinkedQueue<>();
        this.poolTerminateWaitQueue = new ConcurrentLinkedQueue<>();

        //step4: atomic fields of pool monitor
        this.monitorVo = new TaskPoolMonitorVo();
        this.workerCount = new AtomicInteger();
        this.taskHoldingCount = new AtomicInteger();
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

        //step6: create task schedule objects
        this.scheduledQueue = new ScheduledTaskQueue(0);
        this.scheduledTaskPeekThread = new PoolScheduledTaskPeekThread();
        this.scheduledTaskPeekThread.start();

        //step7: create exec factories
        this.joinExecFactory = new JoinExecFactory(this);
        this.onceExecFactory = new OnceExecFactory(this);
        this.scheduledExecFactory = new ScheduledExecFactory(this);

        //step8: set pool to be running state(ready to accept tasks)
        this.poolState = POOL_RUNNING;
    }

    //***************************************************************************************************************//
    //                                       2: Once task submit(2)                                                  //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException {
        return submit(task, (BeeTaskCallback) null);
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException {
        //1: create task config
        TaskConfig config = new TaskConfig(task);
        config.setCallback(callback);
        //2: create task handle
        BaseHandle handle = onceExecFactory.createHandle(config);

        //3: push task handle to queue
        this.pushToExecutionQueue(handle);
        //4:return handle to caller
        return handle;
    }

    //***************************************************************************************************************//
    //                                  3: Join task submit(1)                                                       //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task, BeeTaskJoinOperator operator) throws BeeTaskException {
        //1: create task config
        TaskConfig config = new TaskConfig(task);
        config.setOperator(operator);

        //2: create task handle
        BaseHandle handle = joinExecFactory.createHandle(config);

        //3: push task handle to queue
        this.pushToExecutionQueue(handle);
        //4:return handle to caller
        return handle;
    }

    //***************************************************************************************************************//
    //                                    4: Scheduled task submit(7)                                                //
    //***************************************************************************************************************//
    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException {
        return addScheduleTask(task, null, unit, delay, 0, false);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException {
        return addScheduleTask(task, null, unit, initialDelay, period, false);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit) throws BeeTaskException {
        return addScheduleTask(task, null, unit, initialDelay, delay, true);
    }

    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        return addScheduleTask(task, null, unit, delay, 0, false);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        return addScheduleTask(task, null, unit, initialDelay, period, false);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException {
        return addScheduleTask(task, null, unit, initialDelay, delay, true);
    }

    private BeeTaskScheduledHandle addScheduleTask(BeeTask task, BeeTaskCallback callback, TimeUnit unit, long initialDelay, long intervalTime, boolean fixedDelay) throws BeeTaskException {
        //1: create task config
        TaskConfig config = new TaskConfig(task);
        config.setCallback(callback);
        config.setTimeUnit(unit);
        config.setInitialDelay(initialDelay);
        config.setIntervalDelay(intervalTime);
        config.setFixedDelay(fixedDelay);
        BeeTaskScheduledHandle handle = (BeeTaskScheduledHandle) scheduledExecFactory.createHandle(config);

        //add task handle to time sortable array,and gets its index in array
        int index = scheduledQueue.add(handle);

//        //re-check pool state,if not in running,then try to cancel
//        if (this.poolState != POOL_RUNNING) {
//            if (handle.setAsCancelled()) {
//                if (scheduledQueue.remove(handle) >= 0) taskHoldingCount.decrementAndGet();
//                throw new TaskRejectedException("Access forbidden,task pool was closed or in clearing");
//            }
//        }

        //if the new handle is first of array,then wakeup peek thread to spy on it
        if (index == 0) wakeupSchedulePeekThread();
        return handle;
    }

    //***************************************************************************************************************//
    //                4: task check(1)                                                                               //
    //***************************************************************************************************************//
    private void taskCheck(BeeTask task, boolean scheduleInd, int scheduleType,
                           long initialDelay, long intervalTime, TimeUnit unit) throws BeeTaskException {
        //1: task test
        if (task == null) throw new BeeTaskException("Task can't be null");
        if (scheduleInd) {

        }

        //2: pool state check
        if (this.poolState != POOL_RUNNING)
            throw new TaskRejectedException("Access forbidden,task pool was closed or in clearing");

        //3: try to increment task count,failed,throws exception
        do {
            int currentCount = taskHoldingCount.get();
            if (currentCount >= maxTaskSize) throw new TaskRejectedException("Pool was full,task rejected");
            if (taskHoldingCount.compareAndSet(currentCount, currentCount + 1)) return;
        } while (true);
    }

    //***************************************************************************************************************//
    //                5: task execution(4)                                                                           //
    //***************************************************************************************************************//
    private void wakeupSchedulePeekThread() {
        LockSupport.unpark(scheduledTaskPeekThread);
    }

    //push task to execution queue(**scheduled peek thread calls this method to push task**)
    private void pushToExecutionQueue(BaseHandle taskHandle) {
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

            //2.2: create a new worker with the task handle
            if (workerCount.compareAndSet(currentCount, currentCount + 1)) {
                PoolWorkerThread worker = new PoolWorkerThread(taskHandle);
                workerQueue.offer(worker);
                worker.start();
                return;
            }
        } while (true);
    }

    //works inside worker thread
    private void executeTask(BaseHandle handle) {
//
//        try {
//            //1: increment running count
//            taskRunningCount.incrementAndGet();//need think of count exceeded?
//
//            //2: execute callback
//            BeeTask task = handle.getTask();
//            BeeTaskCallback callback = handle.getCallback();
//            if (callback != null) {
//                try {
//                    callback.beforeCall(handle);
//                } catch (Throwable e) {
//                    //do nothing
//                }
//            }
//
//            //3: execute task
//            try {
//                handle.setDone(TASK_CALL_RESULT, task.call());
//            } catch (Throwable e) {
//                handle.setDone(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
//            }
//        } finally {
//            taskRunningCount.decrementAndGet();
//            if (handle instanceof ScheduledTaskHandle) {
//                ScheduledTaskHandle scheduledHandle = (ScheduledTaskHandle) handle;
//                if (scheduledHandle.isPeriodic()) {
//                    scheduledHandle.prepareForNextCall();//reset to waiting state for next execution
//                    if (scheduledQueue.add(scheduledHandle) == 0)
//                        wakeupSchedulePeekThread();
//                } else {//one scheduled task,so end
//                    taskCompletedCount.incrementAndGet();
//                }
//            } else {
//                taskCompletedCount.incrementAndGet();
//            }
//        }
    }

    //remove from array or queue(method called inside handle)
    public void removeCancelledTask(BeeTaskHandle handle) {
        if (handle instanceof ScheduledTaskHandle) {
            int taskIndex = scheduledQueue.remove((ScheduledTaskHandle) handle);
            if (taskIndex >= 0) taskHoldingCount.decrementAndGet();//task removed successfully by call thread
            if (taskIndex == 0) wakeupSchedulePeekThread();
        } else if (executionQueue.remove(handle)) {
            taskHoldingCount.decrementAndGet();
        }
    }

    //***************************************************************************************************************//
    //                6: Pool clear (3)                                                                              //
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
            if (checkedConfig != null) {
                this.maxTaskSize = checkedConfig.getTaskMaxSize();
                this.maxWorkerSize = checkedConfig.getMaxWorkerSize();
                this.workerInDaemon = checkedConfig.isWorkInDaemon();
                this.workerKeepAliveTime = MILLISECONDS.toNanos(checkedConfig.getWorkerKeepAliveTime());
                this.workerKeepaliveTimed = workerKeepAliveTime > 0;
                //reinitialize worker thread
                int workerInitSize = config.getInitWorkerSize();
                this.workerCount.set(workerInitSize);
                for (int i = 0; i < workerInitSize; i++) {
                    PoolWorkerThread worker = new PoolWorkerThread(WORKER_WORKING);
                    workerQueue.offer(worker);
                    worker.start();
                }
            }

            this.poolState = POOL_RUNNING;
            LockSupport.unpark(this.scheduledTaskPeekThread);
            return true;
        } else {
            return false;
        }
    }

    private List<BeeTask> removeAll(boolean mayInterruptIfRunning) {

//        List<BeeTask> unRunningTaskList = new LinkedList<>();
//        //1: remove scheduled tasks
//        for (ScheduledTaskHandle handle : scheduledQueue.clearAll()) {
//            if (handle.setAsCancelled()) {//collect cancelled tasks by pool
//                unRunningTaskList.add(handle.getTask());
//                handle.setDone(TASK_CANCELLED, null);
//            }
//        }
//
//        //2: remove generic tasks
//        BaseHandle handle;
//        while ((handle = executionQueue.poll()) != null) {
//            if (handle.setAsCancelled()) {//collect cancelled tasks by pool
//                unRunningTaskList.add(handle.getTask());
//                handle.setDone(TASK_CANCELLED, null);
//            }
//        }

        //3: remove generic tasks
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
        return null;
    }

    //***************************************************************************************************************//
    //                7: Pool shutdown (4)                                                                           //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return poolState == POOL_TERMINATED;
    }

    public boolean isTerminating() {
        return poolState == POOL_TERMINATING;
    }

    public List<BeeTask> terminate(boolean mayInterruptIfRunning) throws BeeTaskPoolException {
        if (PoolStateUpd.compareAndSet(this, POOL_RUNNING, POOL_TERMINATING)) {
            List<BeeTask> unCompleteList = this.removeAll(mayInterruptIfRunning);

            this.poolState = POOL_TERMINATED;
            for (Thread thread : poolTerminateWaitQueue)
                LockSupport.unpark(thread);
            return unCompleteList;
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
    //                8: Pool monitor(1)                                                                             //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setPoolState(this.poolState);
        monitorVo.setWorkerCount(workerCount.get());
        monitorVo.setTaskHoldingCount(taskHoldingCount.get());
        monitorVo.setTaskRunningCount(taskRunningCount.get());
        monitorVo.setTaskCompletedCount(taskCompletedCount.get());
        return monitorVo;
    }

    //***************************************************************************************************************//
    //                9: Pool worker class and scheduled class (2)                                                   //
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
                BaseHandle handle = null;
                if (state instanceof BaseHandle) {
                    handle = (BaseHandle) state;
                    state = WORKER_WORKING;
                    this.workState.set(WORKER_WORKING);
                }
                if (handle == null) handle = executionQueue.poll();

                //3: task count
                if (handle != null) {
                    if (handle instanceof ScheduledTaskHandle) {
                        ScheduledTaskHandle scheduledHandle = (ScheduledTaskHandle) handle;
                        if (!scheduledHandle.isPeriodic())
                            taskHoldingCount.decrementAndGet();//time once task,so decrement
                    } else {
                        taskHoldingCount.decrementAndGet();
                    }

                    //4: execute task
                    //if (handle.setAsRunning()) executeTask(handle);
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
                    Object polledObject = scheduledQueue.pollExpired();
                    //2: if polled object is expired schedule task
                    if (polledObject instanceof ScheduledTaskHandle) {
                        ScheduledTaskHandle taskHandle = (ScheduledTaskHandle) polledObject;
                        if (taskHandle.curState.get() == TASK_WAITING)
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
