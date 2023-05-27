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
import org.stone.beetp.BeeTaskCallback;
import org.stone.beetp.BeeTaskException;
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.pool.exception.TaskCancelledException;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.TaskPoolStaticUtil.*;

/**
 * generic task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskExecuteHandle implements BeeTaskHandle {
    final AtomicInteger curState;//reset to waiting state after execution when current task is periodic
    private final BeeTask task;
    private final TaskExecutionPool pool;
    private final BeeTaskCallback callback;
    private final ConcurrentLinkedQueue<Thread> waitQueue;//queue of waiting for task result(maybe exception)

    Object curResult;//it is an exception when task sate code equals<em>TASK_EXCEPTION</em>;
    private volatile Thread workerThread;//set before execution by pool worker and reset to null after execution

    //***************************************************************************************************************//
    //                1: constructor(1)                                                                              //
    //***************************************************************************************************************//
    TaskExecuteHandle(BeeTask task, BeeTaskCallback callback, TaskExecutionPool pool) {
        this.task = task;
        this.pool = pool;
        this.callback = callback;
        this.curState = new AtomicInteger(TASK_WAITING);
        this.waitQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                2: curState methods(6)                                                                         //
    //***************************************************************************************************************//
    public boolean isWaiting() {
        return curState.get() == TASK_WAITING;
    }

    public boolean isExecuting() {
        return curState.get() == TASK_EXECUTING;
    }

    public boolean isDone() {
        return curState.get() > TASK_EXECUTING;
    }

    public boolean isCancelled() {
        return curState.get() == TASK_CANCELLED;
    }

    public boolean isCallResult() {
        return curState.get() == TASK_RESULT;
    }

    public boolean isCallException() {
        return curState.get() == TASK_EXCEPTION;
    }

    //***************************************************************************************************************//
    //                3: task cancel(1)                                                                              //
    //***************************************************************************************************************//
    public boolean cancel(boolean mayInterruptIfRunning) {
        int taskStateCode = curState.get();
        //1: update task state to be cancelled via cas
        if ((setAsCancelled())) {
            this.setDone(TASK_CANCELLED, null);//if exists result waiters,wakeup them
            pool.removeCancelledTask(this);//remove the cancelled task from pool
            return true;
        }

        //2: interrupt task execution thread when it is in blocking state
        if (mayInterruptIfRunning && curState.get() == TASK_EXECUTING && workerThread != null) {
            Thread.State threadState = workerThread.getState();
            if (threadState == Thread.State.WAITING || threadState == Thread.State.TIMED_WAITING)
                workerThread.interrupt();
        }
        return false;
    }

    //***************************************************************************************************************//
    //                4: result getting methods(3)                                                                   //
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
        int taskStateCode = curState.get();
        if (taskStateCode == TASK_RESULT) return curResult;
        if (taskStateCode == TASK_EXCEPTION) throw (BeeTaskException) curResult;
        if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");

        Thread currentThread = Thread.currentThread();
        waitQueue.offer(currentThread);
        boolean timed = nanoseconds > 0;
        long deadline = timed ? System.nanoTime() + nanoseconds : 0;

        try {
            do {
                //read task result,if done,then return
                taskStateCode = curState.get();
                if (taskStateCode == TASK_RESULT) return curResult;
                if (taskStateCode == TASK_EXCEPTION) throw (BeeTaskException) curResult;
                if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");

                //if not done,then waiting until done, timeout,or interrupted
                if (timed) {
                    long parkTime = deadline - System.nanoTime();
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
            waitQueue.remove(currentThread);
        }
    }

    //***************************************************************************************************************//
    //                5: task and callback(2)                                                                        //
    //***************************************************************************************************************//
    BeeTask getTask() {
        return task;
    }

    BeeTaskCallback getCallback() {
        return callback;
    }

    //***************************************************************************************************************//
    //               6: task state CAS(2)                                                                            //
    //**************************************************e************************************************************//
    //only task in waiting can be cancelled
    boolean setAsCancelled() {
        return curState.compareAndSet(TASK_WAITING, TASK_CANCELLED);
    }

    //set to be running state by pool worker before task execution
    boolean setAsRunning() {
        if (curState.compareAndSet(TASK_WAITING, TASK_EXECUTING)) {
            this.workerThread = Thread.currentThread();
            return true;
        }
        return false;
    }

    //***************************************************************************************************************//
    //                7: task done methods(2)                                                                        //
    //***************************************************************************************************************//
    void setDone(int state, Object response) {
        this.curResult = response;
        this.curState.set(state);
        this.wakeupWaitersInGetting();

        if (this.callback != null) {
            try {
                callback.onCallDone(state, response, this);
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    private void wakeupWaitersInGetting() {
        for (Thread thread : waitQueue)
            LockSupport.unpark(thread);

        this.workerThread = null;
    }
}
