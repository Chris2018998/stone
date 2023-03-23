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

import org.stone.beetp.BeeTask;
import org.stone.beetp.BeeTaskException;
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.pool.exception.ResultGetTimeoutException;
import org.stone.beetp.pool.exception.TaskCancelledException;

import java.security.InvalidParameterException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolStaticCenter.*;

/**
 * Task Handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskHandleImpl implements BeeTaskHandle {
    private BeeTask task;
    private Thread workThread;
    private TaskExecutionPool pool;

    private Object result;
    private BeeTaskException exception;
    private AtomicInteger taskState;
    private ConcurrentLinkedQueue<Thread> waitQueue;

    //***************************************************************************************************************//
    //                1: task constructor(1)                                                                         //                                                                                  //
    //***************************************************************************************************************//
    TaskHandleImpl(BeeTask task, TaskExecutionPool pool) {
        this.task = task;
        this.pool = pool;
        this.taskState = new AtomicInteger(TASK_NEW);
        this.waitQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                2: task taskState methods(5)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public boolean isNew() {
        return taskState.get() == TASK_NEW;
    }

    public boolean isRunning() {
        return taskState.get() == TASK_RUNNING;
    }

    public boolean isCancelled() {
        return taskState.get() == TASK_CANCELLED;
    }

    public boolean isCompleted() {
        return taskState.get() == TASK_COMPLETED;
    }

    public boolean isExceptional() {
        return taskState.get() == TASK_EXCEPTIONAL;
    }

    //***************************************************************************************************************//
    //                3: task result get and cancel methods(4)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public Object get() throws BeeTaskException, InterruptedException {
        return get(0);
    }

    public Object get(long timeout, TimeUnit unit) throws BeeTaskException, InterruptedException {
        if (timeout <= 0) throw new InvalidParameterException("Time out value must be greater than zero");
        if (unit == null) throw new InvalidParameterException("Time unit can't be null");
        return get(unit.toNanos(timeout));
    }

    private Object get(long nanoseconds) throws BeeTaskException, InterruptedException {
        int taskStateCode = taskState.get();
        if (taskStateCode == TASK_COMPLETED) return result;
        if (taskStateCode == TASK_EXCEPTIONAL) throw exception;
        if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task was in cancelled state");

        Thread currentThread = Thread.currentThread();
        waitQueue.offer(currentThread);

        boolean timed = nanoseconds > 0;
        long deadline = System.nanoTime() + nanoseconds;

        try {
            do {
                taskStateCode = taskState.get();
                if (taskStateCode == TASK_COMPLETED) return result;
                if (taskStateCode == TASK_EXCEPTIONAL) throw exception;
                if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");

                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime > 0)
                        LockSupport.parkNanos(parkTime);
                    else
                        throw new ResultGetTimeoutException("Get timeout");
                } else {
                    LockSupport.park();
                }

                if (currentThread.isInterrupted()) throw new InterruptedException();
            } while (true);
        } finally {
            waitQueue.remove(currentThread);
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) throws BeeTaskException {
        int taskStateCode = taskState.get();
        if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task was in cancelled state");
        if (taskStateCode == TASK_COMPLETED || taskStateCode == TASK_EXCEPTIONAL)
            throw new TaskCancelledException("Task was already in cancelled taskState");

        //1: try to cas state to cancelled from new
        if (taskStateCode == TASK_NEW && taskState.compareAndSet(TASK_NEW, TASK_CANCELLED)) {
            //pool.remove(this);//@todo task need be removed from pool after cancelled
            return true;
        }

        //2: try to interrupt worker thread(an execution failed exception will set back to the handle by worker thread)
        if (mayInterruptIfRunning && taskState.get() == TASK_RUNNING && workThread != null)
            workThread.interrupt();

        return false;
    }

    //***************************************************************************************************************//
    //                4: driven by worker thread methods(6)                                                          //                                                                                  //
    //***************************************************************************************************************//
    BeeTask getTask() {
        return task;
    }

    boolean compareAndSetState(int expect, int update) {
        return taskState.compareAndSet(expect, update);
    }

    //set by worker thread after setting task state from NEW to RUNNING
    void setWorkThread(Thread workThread) {
        this.workThread = workThread;
    }

    void setResult(Object result) {
        this.result = result;
        this.wakeupWaiters();
    }

    void setException(BeeTaskException exception) {
        this.exception = exception;
        this.wakeupWaiters();
    }

    private void wakeupWaiters() {
        for (Thread thread : waitQueue)
            LockSupport.unpark(thread);
        this.task = null;
        this.pool = null;
        this.workThread = null;
    }
}
