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

import static org.stone.beetp.pool.TaskPoolConstants.*;

/**
 * Task Handle
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class BaseHandle implements BeeTaskHandle {
    final AtomicInteger state;//reset to waiting state after execution when current task is periodic
    final ConcurrentLinkedQueue<Thread> waitQueue;//queue of waiting for task result(maybe exception)
    final TaskPoolImplement pool;
    private final BeeTask task;
    private final boolean isRoot;
    private final BeeTaskCallback callback;
    Object result;//it is an exception when task sate code equals<em>TASK_CALL_EXCEPTION</em>;
    private volatile Thread workThread;//set before execution by pool worker and reset to null after execution

    //***************************************************************************************************************//
    //                                 1: constructor(1)                                                             //
    //***************************************************************************************************************//
    BaseHandle(BeeTask task, BeeTaskCallback callback, TaskPoolImplement pool, boolean isRoot) {
        this.task = task;
        this.pool = pool;
        this.callback = callback;
        this.state = new AtomicInteger(TASK_WAITING);
        this.isRoot = isRoot;
        this.waitQueue = isRoot ? new ConcurrentLinkedQueue<>() : null;
    }

    //***************************************************************************************************************//
    //                               2: task/factory(2)                                                              //
    //***************************************************************************************************************//
    BeeTask getTask() {
        return task;
    }

    boolean isRoot() {
        return isRoot;
    }

    BeeTaskCallback getCallback() {
        return callback;
    }

    //***************************************************************************************************************//
    //                              3: state get methods(6)                                                          //
    //***************************************************************************************************************//
    public boolean isWaiting() {
        return state.get() == TASK_WAITING;
    }

    public boolean isExecuting() {
        return state.get() == TASK_EXECUTING;
    }

    public boolean isDone() {
        return state.get() > TASK_EXECUTING;
    }

    public boolean isCancelled() {
        return state.get() == TASK_CANCELLED;
    }

    public boolean isCallResult() {
        return state.get() == TASK_CALL_RESULT;
    }

    public boolean isCallException() {
        return state.get() == TASK_CALL_EXCEPTION;
    }

    //***************************************************************************************************************//
    //                                  4: task state CAS(2)                                                         //
    //**************************************************e************************************************************//
    boolean setAsCancelled() {
        return state.compareAndSet(TASK_WAITING, TASK_CANCELLED);
    }

    boolean setAsRunning() {//called by work thread
        if (state.compareAndSet(TASK_WAITING, TASK_EXECUTING)) {
            this.workThread = Thread.currentThread();
            return true;
        }
        return false;
    }

    //***************************************************************************************************************//
    //                                  5: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(boolean mayInterruptIfRunning) {
        //1: update task state to be cancelled via cas
        if (setAsCancelled()) {
            this.setDone(TASK_CANCELLED, null);//if exists result waiters,wakeup them
            pool.removeCancelledTask(this);//remove the cancelled task from pool
            return true;
        }

        //2: interrupt task execution thread when it is in blocking state
        if (mayInterruptIfRunning && state.get() == TASK_EXECUTING && workThread != null) {
            Thread.State threadState = workThread.getState();
            if (threadState == Thread.State.WAITING || threadState == Thread.State.TIMED_WAITING)
                workThread.interrupt();
        }
        return false;
    }

    //***************************************************************************************************************//
    //                                 6: result getting methods(3)                                                  //
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
        int stateCode = state.get();
        if (stateCode == TASK_CALL_RESULT) return result;
        this.checkFailureState(stateCode);

        Thread currentThread = Thread.currentThread();
        waitQueue.offer(currentThread);
        boolean timed = nanoseconds > 0;
        long deadline = timed ? System.nanoTime() + nanoseconds : 0;

        try {
            do {
                //read task result,if done,then return
                stateCode = state.get();
                if (stateCode == TASK_CALL_RESULT) return result;
                this.checkFailureState(stateCode);

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

    private void checkFailureState(int state) throws BeeTaskException {
        if (state == TASK_CALL_EXCEPTION) throw (BeeTaskException) result;
        if (state == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");
    }

    //***************************************************************************************************************//
    //                              7: task done methods(2)                                                          //
    //***************************************************************************************************************//
    void setDone(int state, Object result) {
        this.result = result;
        this.state.set(state);
        if (waitQueue != null) this.wakeupWaitersInGetting();

        if (this.callback != null) {
            try {
                callback.afterCall(state, result, this);
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    void wakeupWaitersInGetting() {
        for (Thread thread : waitQueue)
            LockSupport.unpark(thread);

        this.workThread = null;
    }
}
