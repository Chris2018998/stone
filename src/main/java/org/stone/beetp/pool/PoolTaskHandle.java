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
public class PoolTaskHandle<V> implements TaskHandle<V> {
    private static final AtomicReferenceFieldUpdater<PoolTaskHandle, Object> StateUpd = ReferenceFieldUpdaterImpl.newUpdater(PoolTaskHandle.class, Object.class, "state");
    private final Task<V> task;
    private final boolean isRoot;
    private final PoolTaskCenter pool;

    //it may be a completion result or a fail exception,this due to state value
    private Object result;
    //task state(if it is an execution worker,that means in running)
    private volatile Object state = TASK_WAITING;

    //aspect around call
    private TaskAspect<V> callAspect;
    //owner buck contains this handle
    private PoolTaskBucket taskBucket;
    //store waiters for call result
    private ConcurrentLinkedQueue<Thread> waitQueue;

    //1:constructor for sub join task
    PoolTaskHandle(Task<V> task, PoolTaskCenter pool) {
        this.task = task;
        this.pool = pool;
        this.isRoot = false;
    }

    //2:constructor for once task,schedule task,root task join task
    PoolTaskHandle(Task<V> task, TaskAspect<V> callAspect, PoolTaskCenter pool) {
        this.task = task;
        this.pool = pool;
        this.isRoot = true;
        this.callAspect = callAspect;
        this.waitQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                  1: constructor(2)                                                            //
    //**************************************************e************************************************************//
    boolean isRoot() {
        return isRoot;
    }

    void setTaskBucket(PoolTaskBucket taskBucket) {
        if (this.taskBucket == null) this.taskBucket = taskBucket;
    }

    //***************************************************************************************************************//
    //                                  2: task states(6)                                                            //
    //**************************************************e************************************************************//
    public boolean isWaiting() {
        return state == TASK_WAITING;
    }

    public boolean isRunning() {
        return state instanceof TaskExecuteWorker;
    }

    //one of completed states
    public boolean isCancelled() {
        return state == TASK_CANCELLED;
    }

    //one of completed states
    public boolean isSucceed() {
        return state == TASK_SUCCEED;
    }

    //one of completed states
    public boolean isFailed() {
        return state == TASK_FAILED;
    }

    public boolean isCompleted() {
        Object curState = this.state;
        return curState == TASK_SUCCEED || curState == TASK_CANCELLED || curState == TASK_FAILED;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        //1: try to change state to cancelled
        if (state == TASK_WAITING && StateUpd.compareAndSet(this, TASK_WAITING, TASK_CANCELLED)) {
            this.fillTaskResult(TASK_CANCELLED, null);
            this.taskBucket.cancel(this, false);//just remove it from bucket
            return true;
        }

        //2: if parameter mayInterruptIfRunning is true then interrupt possible blocking
        if (mayInterruptIfRunning) {
            Object curState = state;
            if (curState instanceof TaskExecuteWorker) {
                TaskExecuteWorker worker = (TaskExecuteWorker) curState;
                return worker.cancel(this, true);//need check worker thread state whether in blocking
            }
        }

        return false;
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
        if (state == TASK_FAILED) throw (TaskException) this.result;
        if (state == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");
    }

    //***************************************************************************************************************//
    //                                 5: fill result(change state and result)                                       //
    //***************************************************************************************************************//
    private void fillTaskResult(Object state, Object result) {
        //1: update result and state
        this.result = result;
        this.state = state;

        //2: wakeup waiters to get result
        if (waitQueue != null) {
            Thread waitThread;
            while ((waitThread = waitQueue.poll()) != null)
                LockSupport.unpark(waitThread);
        }
    }

    //***************************************************************************************************************//
    //                              6: task execution(3)                                                             //
    //***************************************************************************************************************//
    //this method can override
    protected void beforeExecute() {
        pool.getRunningCount().increment();
    }

    //this method can override
    protected void afterExecute() {
        pool.getRunningCount().decrement();
        pool.getCompletedCount().increment();
        pool.getTaskCount().decrement();
    }

    /**
     * core method to execute task
     */
    protected void executeTask(TaskExecuteWorker worker) {
        if (!StateUpd.compareAndSet(this, TASK_WAITING, worker)) return;

        Object result = null;
        boolean succeed = true;//assume call success

        try {
            //1: call beforeExecute method
            this.beforeExecute();

            //2: execute beforeCall method of aspect
            if (callAspect != null)
                callAspect.beforeCall(this);

            //3: execute call of task(** key step **)
            result = this.invokeTaskCall();
            //System.out.println("result:"+result);
        } catch (Throwable e) {
            succeed = false;
            result = new TaskExecutionException(e);
        } finally {
            //4: fill result and wakeup waiters if exists
            this.fillTaskResult(succeed ? TASK_SUCCEED : TASK_FAILED, result);

            //5: call afterExecute method
            this.afterExecute();

            //6: execute afterCall method of aspect
            if (callAspect != null)
                callAspect.afterCall(succeed, result, this);
        }
    }

    //***************************************************************************************************************//
    //                              7: others                                                                        //
    //***************************************************************************************************************//
    void afterSetResult(int state, Object result) {
    }

    Object invokeTaskCall() throws Exception {
        return task.call();
    }

//        //2: execute callAspect
//        if (this.callAspect != null) {
//            try {
//                this.callAspect.afterCall(state, result, this);
//            } catch (final Throwable e) {
//                System.err.println("Failed to execute callAspect.afterCall");
//            }
//        }
//
//        //3: plugin method call
//        this.afterSetResult(state, result);
}
