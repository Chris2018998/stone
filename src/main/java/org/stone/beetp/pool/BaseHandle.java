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
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.BeeTaskStates.*;

/**
 * Task base Handle
 *
 * @author Chris Liao
 * @version 1.0
 */
class BaseHandle implements BeeTaskHandle {
    private static final AtomicIntegerFieldUpdater<BaseHandle> StateUpd = AtomicIntegerFieldUpdater.newUpdater(BaseHandle.class, "state");
    protected final BeeTask task;
    protected final boolean isRoot;
    protected final TaskPoolImplement pool;
    private final BeeTaskCallback callback;
    private final ConcurrentLinkedQueue<Thread> waitQueue;

    protected Object result;
    protected volatile int state;
    private volatile TaskWorkThread workThread;//set before execution by pool worker and reset to null after execution

    //***************************************************************************************************************//
    //                                 1: constructor(1)                                                             //
    //***************************************************************************************************************//
    BaseHandle(BeeTask task, BeeTaskCallback callback, boolean isRoot, TaskPoolImplement pool) {
        this.task = task;
        this.isRoot = isRoot;//for join task
        this.pool = pool;
        this.callback = callback;
        this.state = TASK_WAITING;
        this.waitQueue = isRoot ? new ConcurrentLinkedQueue<Thread>() : null;
    }

    //***************************************************************************************************************//
    //                               2: other(2)                                                                     //
    //***************************************************************************************************************//
    BeeTask getTask() {
        return task;
    }

    boolean isRoot() {
        return this.isRoot;
    }

    //***************************************************************************************************************//
    //                              3: state get methods(6)                                                          //
    //***************************************************************************************************************//
    public boolean isWaiting() {
        return this.state == TASK_WAITING;
    }

    public boolean isExecuting() {
        return this.state == TASK_EXECUTING;
    }

    public boolean isDone() {
        return this.state > TASK_EXECUTING;
    }

    public boolean isCancelled() {
        return this.state == TASK_CANCELLED;
    }

    public boolean isCallResult() {
        return this.state == TASK_CALL_RESULT;
    }

    public boolean isCallException() {
        return this.state == TASK_CALL_EXCEPTION;
    }

    //***************************************************************************************************************//
    //                                  4: task state CAS(2)                                                         //
    //**************************************************e************************************************************//
    boolean setAsCancelled() {
        return StateUpd.compareAndSet(this, TASK_WAITING, TASK_CANCELLED);
    }

    boolean setAsRunning(TaskWorkThread workThread) {//called by work thread
        if (StateUpd.compareAndSet(this, TASK_WAITING, TASK_EXECUTING)) {
            this.workThread = workThread;
            return true;
        }
        return false;
    }

    //***************************************************************************************************************//
    //                                  5: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        //1: update task state to be cancelled via cas
        if (this.setAsCancelled()) {
            this.setResult(TASK_CANCELLED, null);//if exists result waiters,wakeup them
            this.pool.removeCancelledTask(this);//remove the cancelled task from pool
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
    //                                 6: result getting methods(3)                                                  //
    //***************************************************************************************************************//
    public Object get() throws BeeTaskException, InterruptedException {
        return this.get(0);
    }

    public Object get(final long timeout, final TimeUnit unit) throws BeeTaskException, InterruptedException {
        if (timeout < 0) throw new IllegalArgumentException("Time out value must be greater than zero");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        return this.get(unit.toNanos(timeout));
    }

    private Object get(final long nanoseconds) throws BeeTaskException, InterruptedException {
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

    private void throwFailureException(final int state) throws BeeTaskException {
        if (state == TASK_CALL_EXCEPTION) throw (BeeTaskException) this.result;
        if (state == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");
    }

    //***************************************************************************************************************//
    //                              7: execute task(5)                                                               //
    //***************************************************************************************************************//
    void beforeExecuteTask() {//default implement for once task
        pool.getTaskHoldingCount().decrementAndGet();
        pool.getTaskRunningCount().incrementAndGet();
    }

    void execute() { //an import method called by pool to execute task
        //1: before execute
        this.beforeExecuteTask();
        //2: execute task
        this.executeInternalTask();
        //3: after execute
        this.afterExecuteTask();
    }

    void afterExecuteTask() {//default implement for once task
        pool.getTaskRunningCount().decrementAndGet();
        pool.getTaskCompletedCount().incrementAndGet();
    }

    //*********************************************** execute call ***************************************************//
    void executeInternalTask() {
        if (callback != null) {
            try {
                callback.beforeCall(this);
            } catch (Throwable e) {
                //do nothing
            }
        }

        try {
            this.setResult(TASK_CALL_RESULT, this.invokeTaskCall());
        } catch (Throwable e) {
            this.setResult(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
        }
    }

    //this method will override in tree handle
    Object invokeTaskCall() throws Exception {
        return task.call();
    }

    //***************************************************************************************************************//
    //                              8: result setting(3)                                                             //
    //***************************************************************************************************************//
    void setResult(final int state, final Object result) {
        //1: set result and try to wakeup waiters if exists
        this.result = result;
        this.state = state;
        this.workThread = null;

        if (isRoot) {
            Thread waitThread;
            while ((waitThread = waitQueue.poll()) != null)
                LockSupport.unpark(waitThread);
        }

        //2: plugin method call
        this.afterSetResult(state, result);

        //3: execute callback
        if (this.callback != null) {
            try {
                this.callback.afterCall(state, result, this);
            } catch (final Throwable e) {
                //do nothing
            }
        }
    }

    //fill incr complete count by join task and tree task
    void afterSetResult(final int state, final Object result) {
    }
}