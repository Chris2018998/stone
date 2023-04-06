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
public class TaskHandleImpl implements BeeTaskHandle {
    private final AtomicInteger taskState;

    private BeeTask task;
    private TaskExecutionPool pool;
    private volatile Thread workThread;

    private Object callResponse;//result,exception
    private ConcurrentLinkedQueue<Thread> waitQueue;

    //***************************************************************************************************************//
    //                1: task constructor(1)                                                                         //                                                                                  //
    //***************************************************************************************************************//
    TaskHandleImpl(BeeTask task, int initState, TaskExecutionPool pool) {
        this.taskState = new AtomicInteger(initState);
        if (initState == TASK_NEW) {
            this.task = task;
            this.pool = pool;
            this.waitQueue = new ConcurrentLinkedQueue<>();
        }
    }

    //***************************************************************************************************************//
    //                2: task taskState methods(2)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public boolean isDone() {
        return taskState.get() > TASK_RUNNING;
    }

    public boolean isCancelled() {
        return taskState.get() == TASK_CANCELLED;
    }

    //***************************************************************************************************************//
    //                3: task result get and cancel methods(4)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public Object get() throws BeeTaskException, InterruptedException {
        return get(0);
    }

    public Object get(long timeout, TimeUnit unit) throws BeeTaskException, InterruptedException {
        if (timeout < 0) throw new IllegalArgumentException("Time out value must be greater than zero");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        return get(unit.toNanos(timeout));
    }

    private Object get(long nanoseconds) throws BeeTaskException, InterruptedException {
        int taskStateCode = taskState.get();
        if (taskStateCode == TASK_CALL_RESULT) return callResponse;
        if (taskStateCode == TASK_EXCEPTIONAL) throw (BeeTaskException) callResponse;
        if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task was in cancelled state");

        Thread currentThread = Thread.currentThread();
        waitQueue.offer(currentThread);
        boolean timed = nanoseconds > 0;
        long deadline = System.nanoTime() + nanoseconds;

        try {
            do {
                taskStateCode = taskState.get();
                if (taskStateCode == TASK_CALL_RESULT) return callResponse;
                if (taskStateCode == TASK_EXCEPTIONAL) throw (BeeTaskException) callResponse;
                if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task was in cancelled state");

                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime > 0)
                        LockSupport.parkNanos(parkTime);
                    else
                        throw new ResultGetTimeoutException("Get timeout");
                } else {
                    LockSupport.park();
                }

                if (Thread.interrupted()) throw new InterruptedException();
            } while (true);
        } finally {
            waitQueue.remove(currentThread);
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        int taskStateCode = taskState.get();
        //1: try to cas state to cancelled from new
        if (taskStateCode == TASK_NEW && taskState.compareAndSet(TASK_NEW, TASK_CANCELLED)) {
            pool.removeExecuteTask(this);
            setDone(TASK_CANCELLED, null);
            return true;
        }

        //2: try to interrupt worker thread(an execution failed exception will set back to the handle by worker thread)
        if (mayInterruptIfRunning && taskState.get() == TASK_RUNNING && workThread != null) {
            Thread.State threadState = workThread.getState();
            if (threadState == Thread.State.WAITING || threadState == Thread.State.TIMED_WAITING)
                workThread.interrupt();
        }
        return false;
    }

    //***************************************************************************************************************//
    //                4: driven by worker thread methods(6)                                                          //                                                                                  //
    //***************************************************************************************************************//
    BeeTask getTask() {
        return task;
    }

    void setState(int update) {
        taskState.set(update);
    }

    boolean compareAndSetState(int expect, int update) {
        return taskState.compareAndSet(expect, update);
    }

    //set by worker thread after setting task state from NEW to RUNNING
    void setWorkThread(Thread workThread) {
        this.workThread = workThread;
    }

    void setDone(int state, Object response) {
        this.callResponse = response;
        this.taskState.set(state);
        this.wakeupWaiters();
    }

    void wakeupWaiters() {
        for (Thread thread : waitQueue)
            LockSupport.unpark(thread);
        this.workThread = null;
        this.task = null;
        this.pool = null;
    }
}
