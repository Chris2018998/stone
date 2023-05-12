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
 * execution handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TaskExecuteHandle implements BeeTaskHandle {
    //null,result,exception
    Object curResult;//can be reset to be null(initialization state)
    AtomicInteger curState;//can be reset to be waiting(initialization state)

    private BeeTask task;
    private TaskExecutionPool pool;
    private BeeTaskCallback callback;
    private volatile Thread workerThread;
    private ConcurrentLinkedQueue<Thread> waitQueue;

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
    //                2: curState methods(3)                                                                         //
    //***************************************************************************************************************//
    public boolean isDone() {
        return curState.get() > TASK_EXECUTING;
    }

    public boolean isExecuting() {
        return curState.get() == TASK_EXECUTING;
    }

    public boolean isCancelled() {
        return curState.get() == TASK_CANCELLED;
    }

    //***************************************************************************************************************//
    //                3: task cancel(1)                                                                              //
    //***************************************************************************************************************//
    public boolean cancel(boolean mayInterruptIfRunning) {
        int taskStateCode = curState.get();
        //1: try to cas curState to cancelled
        if ((taskStateCode == TASK_WAITING) && curState.compareAndSet(taskStateCode, TASK_CANCELLED)) {
            this.setDone(TASK_CANCELLED, null);//wakeup waiters of result getting
            pool.removeCancelledTask(this);//remove from pool
            return true;
        }

        //2: try to interrupt worker thread(an execution failed exception will set back to the handle by worker thread)
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
                taskStateCode = curState.get();
                if (taskStateCode == TASK_RESULT) return curResult;
                if (taskStateCode == TASK_EXCEPTION) throw (BeeTaskException) curResult;
                if (taskStateCode == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");

                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime > 0)
                        LockSupport.parkNanos(parkTime);
                    else
                        throw new TaskResultGetTimeoutException();
                } else {
                    LockSupport.park();
                }

                if (Thread.interrupted()) throw new InterruptedException();
            } while (true);
        } finally {
            waitQueue.remove(currentThread);
        }
    }

    //***************************************************************************************************************//
    //                5: driven by worker thread methods(2)                                                          //
    //***************************************************************************************************************//
    BeeTask getTask() {
        return task;
    }

    BeeTaskCallback getCallback() {
        return callback;
    }

    //***************************************************************************************************************//
    //               6: task curState setting/getting(4)                                                             //
    //**************************************************e************************************************************//
    int getCurState() {
        return curState.get();
    }

    void setCurState(int update) {
        curState.set(update);
    }

    boolean compareAndSetState(int expect, int update) {
        return curState.compareAndSet(expect, update);
    }

    boolean setAsRunning() {//call in worker thread after task polled from queue
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
        this.task = null;
        this.pool = null;
    }
}
