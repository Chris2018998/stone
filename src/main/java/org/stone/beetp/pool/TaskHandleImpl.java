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
import org.stone.beetp.pool.exception.GetTimeoutException;
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
    private Object result;
    private BeeTaskException exception;
    private AtomicInteger state = new AtomicInteger(TASK_NEW);
    private ConcurrentLinkedQueue<Thread> waitQueue = new ConcurrentLinkedQueue<>();

    //***************************************************************************************************************//
    //                1: task state methods(5)                                                                       //                                                                                  //
    //***************************************************************************************************************//
    public boolean isNew() {
        return state.get() == TASK_NEW;
    }

    public boolean isRunning() {
        return state.get() == TASK_RUNNING;
    }

    public boolean isCancelled() {
        return state.get() == TASK_CANCELLED;
    }

    public boolean isCompleted() {
        return state.get() == TASK_COMPLETED;
    }

    public boolean isExceptional() {
        return state.get() == TASK_EXCEPTIONAL;
    }

    //***************************************************************************************************************//
    //                2: task result get and cancel methods(3)                                                       //                                                                                  //
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
        int stateCode = state.get();
        if (stateCode == TASK_COMPLETED) return result;
        if (stateCode == TASK_EXCEPTIONAL) throw exception;
        if (stateCode == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");

        Thread currentThread = Thread.currentThread();
        waitQueue.offer(currentThread);

        boolean timed = nanoseconds > 0;
        long deadline = System.nanoTime() + nanoseconds;

        try {
            do {
                stateCode = state.get();
                if (stateCode == TASK_COMPLETED) return result;
                if (stateCode == TASK_EXCEPTIONAL) throw exception;
                if (stateCode == TASK_CANCELLED) throw new TaskCancelledException("Task has been cancelled");

                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime > 0)
                        LockSupport.parkNanos(parkTime);
                    else
                        throw new GetTimeoutException("Get timeout");
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
        return true;
    }

    //***************************************************************************************************************//
    //                3: package access methods(3)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    boolean compareAndSetState(int expect, int update) {
        return state.compareAndSet(expect, update);
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
        for (Thread thread : waitQueue) {
            LockSupport.unpark(thread);
        }
    }
}
