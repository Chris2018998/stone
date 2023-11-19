/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.execution;

import org.stone.beetp.Task;
import org.stone.beetp.TaskCallback;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.exception.TaskCancelledException;
import org.stone.beetp.exception.TaskException;
import org.stone.beetp.exception.TaskExecutionException;
import org.stone.beetp.exception.TaskResultGetTimeoutException;
import org.stone.tools.unsafe.UnsafeAdaptorSunMiscImpl;
import sun.misc.Unsafe;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.TaskStates.*;

/**
 * Base Implementation of task handle interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
class BaseHandle implements TaskHandle {
    //cas unsafe for state field
    private static final Unsafe U;
    private static final long stateOffset;

    static {
        try {
            U = UnsafeAdaptorSunMiscImpl.U;
            stateOffset = U.objectFieldOffset(BaseHandle.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    final Task task;
    final boolean isRoot;
    final TaskExecutionPool pool;
    Object result;//computed result or an execution exception
    volatile int state;//0 is default value(TASK_WAITING)

    private TaskCallback callback;
    private TaskWorkThread workThread;//set before execution,clear after execution
    private ConcurrentLinkedQueue<Thread> waitQueue;

    //1:constructor for sub join task
    BaseHandle(Task task, TaskExecutionPool pool) {
        this.task = task;
        this.pool = pool;
        this.isRoot = false;
    }

    //2:constructor for once task,schedule task,root join task
    BaseHandle(Task task, TaskCallback callback, TaskExecutionPool pool) {
        this.task = task;
        this.pool = pool;
        this.isRoot = true;
        this.callback = callback;
        this.waitQueue = new ConcurrentLinkedQueue<Thread>();
    }

    //***************************************************************************************************************//
    //                                  1: task state                                                                //
    //**************************************************e************************************************************//
    public int getState() {
        return state;
    }

    final boolean setAsCancelled() {
        return U.compareAndSwapInt(this, stateOffset, TASK_WAITING, TASK_CANCELLED);
    }

    final boolean setAsRunning(TaskWorkThread thread) {
        if (U.compareAndSwapInt(this, stateOffset, TASK_WAITING, TASK_EXECUTING)) {
            thread.curTaskHandle = this;
            this.workThread = thread;
            return true;
        }
        return false;
    }

    //***************************************************************************************************************//
    //                                  2: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        //1: update task state to be cancelled via cas
        if (this.setAsCancelled()) {
            this.setResult(TASK_CANCELLED, null);//if exists result waiters,wakeup them
            this.pool.removeCancelledTask(this);//remove the cancelled task from execution
            return true;
        }

        //2: interrupt task execution thread when it is in blocking state
        final TaskWorkThread executeThread = this.workThread;
        if (mayInterruptIfRunning && this.state == TASK_EXECUTING && executeThread != null) {
            final Thread.State threadState = executeThread.getState();
            if ((threadState == Thread.State.WAITING || threadState == Thread.State.TIMED_WAITING) && this.state == TASK_EXECUTING)
                executeThread.interrupt(this);//cas maybe better?
        }

        return false;
    }

    //***************************************************************************************************************//
    //                                 3: result getting methods(3)                                                  //
    //***************************************************************************************************************//
    public Object get() throws TaskException, InterruptedException {
        return this.get(0);
    }

    public Object get(final long timeout, final TimeUnit unit) throws TaskException, InterruptedException {
        if (timeout < 0) throw new IllegalArgumentException("Time out must be greater than zero");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        return this.get(unit.toNanos(timeout));
    }

    private Object get(final long nanoseconds) throws TaskException, InterruptedException {
        int stateCode = this.state;
        if (stateCode == TASK_CALL_RESULT) return this.result;
        this.throwFailureException(stateCode);

        final Thread currentThread = Thread.currentThread();
        this.waitQueue.offer(currentThread);
        final boolean timed = nanoseconds > 0;
        final long deadline = timed ? System.nanoTime() + nanoseconds : 0;

        try {
            do {
                //read task result,if done,then return
                stateCode = this.state;
                if (stateCode == TASK_CALL_RESULT) return this.result;
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

    private void throwFailureException(final int state) throws TaskException {
        if (state == TASK_CALL_EXCEPTION) throw (TaskException) this.result;
        if (state == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");
    }

    //***************************************************************************************************************//
    //                              4: execute task(4)                                                               //
    //***************************************************************************************************************//
    void beforeExecute() {
        pool.getTaskHoldingCount().decrementAndGet();
        pool.getTaskRunningCount().incrementAndGet();
    }

    void afterExecute() {
        pool.getTaskRunningCount().decrementAndGet();
        pool.getTaskCompletedCount().incrementAndGet();
    }

    Object invokeTaskCall() throws Exception {
        return task.call();
    }

    void executeTask() {
        if (callback != null) {
            try {
                callback.beforeCall(this);
            } catch (Throwable e) {
                System.err.println("Failed to execute callback.beforeCall");
            }
        }

        try {
            this.setResult(TASK_CALL_RESULT, this.invokeTaskCall());
        } catch (Throwable e) {
            this.setResult(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
        }
    }

    //***************************************************************************************************************//
    //                              5: result setting(2)                                                             //
    //***************************************************************************************************************//
    void setResult(int state, Object result) {
        //1: set result and try to wakeup waiters if exists
        this.result = result;
        this.workThread = null;
        if (isRoot) this.state = state;

        if (waitQueue != null) {
            Thread waitThread;
            while ((waitThread = waitQueue.poll()) != null)
                LockSupport.unpark(waitThread);
        }

        //2: execute callback
        if (this.callback != null) {
            try {
                this.callback.afterCall(state, result, this);
            } catch (final Throwable e) {
                System.err.println("Failed to execute callback.afterCall");
            }
        }

        //3: plugin method call
        this.afterSetResult(state, result);
    }

    //fill incr complete count by join task and tree task
    void afterSetResult(int state, Object result) {

    }
}
