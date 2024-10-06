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

import org.stone.beetp.Task;
import org.stone.beetp.TaskAspect;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.pool.exception.TaskCancelledException;
import org.stone.beetp.pool.exception.TaskException;
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * Impl of task handle
 *
 * @author Chris Liao
 * @version 1.0
 */
class PoolTaskHandle<V> implements TaskHandle<V> {
    protected static final AtomicReferenceFieldUpdater<PoolTaskHandle, Object> StateUpd = ReferenceFieldUpdaterImpl.newUpdater(PoolTaskHandle.class, Object.class, "state");
    protected final Task<V> task;
    protected final PoolTaskCenter pool;
    private final TaskAspect<V> callAspect;
    private final ConcurrentLinkedQueue<Thread> waitQueue;

    //it may be a result or a fail exception,this due to state value
    protected Object result;
    //task state(if it is an execution worker,which means that task in being executing)
    protected volatile Object state;
    //task bucket of task
    protected ConcurrentLinkedQueue<PoolTaskHandle<?>> taskBucket;

    PoolTaskHandle(Task<V> task, TaskAspect<V> callAspect, PoolTaskCenter pool, boolean supportWait) {
        this.task = task;
        this.pool = pool;
        this.callAspect = callAspect;
        this.waitQueue = supportWait ? new ConcurrentLinkedQueue<>() : null;

        this.state = TASK_WAITING;
    }


    //***************************************************************************************************************//
    //                                  1: task bucket(1)                                                            //
    //**************************************************e************************************************************//
    void setTaskBucket(ConcurrentLinkedQueue<PoolTaskHandle<?>> taskBucket) {
        this.taskBucket = taskBucket;
    }

    //***************************************************************************************************************//
    //                                  2: task states(6)                                                            //
    //**************************************************e************************************************************//
    public boolean isWaiting() {
        return state == TASK_WAITING;
    }

    public boolean isRunning() {
        return state instanceof TaskExecutionWorker;
    }

    //one of completed states
    public boolean isCancelled() {
        return state == TASK_CANCELLED;
    }

    //one of completed states
    public boolean isSuccessful() {
        return state == TASK_SUCCEED;
    }

    //one of completed states
    public boolean isExceptional() {
        return state == TASK_EXCEPTIONAL;
    }

    public boolean isCompleted() {
        Object curState = this.state;
        return curState == TASK_SUCCEED || curState == TASK_CANCELLED || curState == TASK_EXCEPTIONAL;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        //1: try to change state to cancelled
        if (state == TASK_WAITING && StateUpd.compareAndSet(this, TASK_WAITING, TASK_CANCELLED)) {
            this.fillTaskResult(TASK_CANCELLED, null);
            pool.decrementTaskCount();
            this.taskBucket.remove(this);
            return true;//cancel successful
        }

        //2: interrupt process
        if (mayInterruptIfRunning) {
            Object curState = state;
            if (curState instanceof TaskExecutionWorker) {//in being executed
                TaskExecutionWorker worker = (TaskExecutionWorker) curState;
                worker.interrupt();//thread interruption can't ensure process exit in time
            }
        }
        return false;
    }

    //***************************************************************************************************************//
    //                              4: task call(5)                                                                  //
    //***************************************************************************************************************//
    boolean setExecutionWorker(TaskExecutionWorker worker) {
        return StateUpd.compareAndSet(this, TASK_WAITING, worker);
    }

    protected void executeTask(TaskExecutionWorker execWorker) {
        try {
            if (callAspect != null) callAspect.beforeCall(this);
            this.fillTaskResult(TASK_SUCCEED, invokeTaskCall());//set success result
        } catch (Throwable e) {
            this.fillTaskResult(TASK_EXCEPTIONAL, new TaskExecutionException(e));//set failure exception
        } finally {
            execWorker.incrementCompletedCount();
            pool.decrementTaskCount();
        }
    }

    protected Object invokeTaskCall() throws Exception {
        return task.call();
    }

    void fillTaskResult(Object state, Object result) {
        //1: update result and state
        this.result = result;
        this.state = state;
        //2: wakeup waiters to get result
        if (waitQueue != null) {
            Thread waitThread;
            while ((waitThread = waitQueue.poll()) != null)
                LockSupport.unpark(waitThread);
        }

        final boolean success = state == TASK_SUCCEED;
        try {
            this.afterExecute(success, result);
        } catch (Throwable e) {
            //e.printStackTrace();
        }

        if (callAspect != null)
            try {
                callAspect.afterCall(success, result, this);
            } catch (Throwable e) {
                //e.printStackTrace();
            }
    }

    protected void afterExecute(boolean success, Object result) {

    }

    //***************************************************************************************************************//
    //                                 4: get call result(4)                                                         //
    //***************************************************************************************************************//
    public V get() throws TaskException, InterruptedException {
        return this.get(0);
    }

    public V get(final long timeout, final TimeUnit unit) throws TaskException, InterruptedException {
        if (timeout < 0) throw new IllegalArgumentException("Timeout must can not be less than zero");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        return this.get(unit.toNanos(timeout));
    }

    private V get(final long nanoseconds) throws TaskException, InterruptedException {
        Object stateCode = this.state;
        if (stateCode == TASK_SUCCEED) return (V) this.result;
        this.throwFailureException(stateCode);

        final Thread currentThread = Thread.currentThread();
        this.waitQueue.offer(currentThread);
        final boolean timed = nanoseconds > 0L;
        final long deadline = timed ? System.nanoTime() + nanoseconds : 0L;

        try {
            do {
                //read task result,if done,then return
                stateCode = this.state;
                if (stateCode == TASK_SUCCEED) return (V) this.result;
                this.throwFailureException(stateCode);

                //if not done,then waiting until done, timeout,or interrupted
                if (timed) {
                    final long parkTime = deadline - System.nanoTime();
                    if (parkTime > 0)
                        LockSupport.parkNanos(parkTime);
                    else
                        throw new TaskResultGetTimeoutException();
                } else {
                    LockSupport.park();
                }

                //if interrupted,then throws exception
                if (Thread.interrupted()) throw new InterruptedException();
            } while (true);
        } finally {
            this.waitQueue.remove(currentThread);
        }
    }

    private void throwFailureException(final Object state) throws TaskException {
        if (state == TASK_EXCEPTIONAL) throw (TaskException) this.result;
        if (state == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");
    }


}
