/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pool task
 *
 * @author Chris Liao
 * @version 1.0
 */
class ThreadPoolTask<V> {
    //task not running
    private static final int State_New = 1;
    //task in executing
    private static final int State_Executing = 2;
    //task done
    private static final int State_Result = 3;
    //task canceled
    private static final int State_Canceled = 4;
    //task execution exception
    private static final int State_ExecutionException = 5;

    //task Id
    private final UUID taskId;
    //call object
    private final Callable<V> call;
    //task state(can be read by future)
    private final AtomicInteger state;
    //fetch result
    private ThreadTaskFuture<V> future;

    //default result set to future when call result is null
    private V defaultResult;
    //true when defaultResult has been set
    private boolean existDefaultResult;
    //call result from callable
    private V result;
    //exception from executing call
    private ExecutionException executionException;

    public ThreadPoolTask(Callable<V> call, ThreadTaskFuture<V> future) {
        this.call = call;
        this.future = future;
        this.taskId = UUID.randomUUID();
        this.state = new AtomicInteger(State_New);
    }

    //****************************************************************************************************************//
    //                                     1: configuration get/set                                                   //
    //****************************************************************************************************************//
    UUID getTaskId() {
        return taskId;
    }

    //runnable call not need bind
    public void bindFuture(ThreadTaskFuture<V> future) {
        this.future = future;
    }

    public boolean isExistDefaultResult() {
        return existDefaultResult;
    }

    public void setDefaultResult(V defaultResult) {
        this.defaultResult = defaultResult;
        this.existDefaultResult = true;
    }

    //****************************************************************************************************************//
    //                                     2: state get/set                                                           //
    //****************************************************************************************************************//
    final boolean isDone() {
        return state.get() >= State_Result;
    }

    final boolean isCancelled() {
        return state.get() == State_Canceled;
    }

    final int getState() {
        return state.get();
    }

    final void setState(int newState) {
        state.set(newState);
    }

    final boolean compareAndSetState(int expect, int update) {
        return state.compareAndSet(expect, update);
    }

    //****************************************************************************************************************//
    //                                     3: result get/set                                                          //
    //****************************************************************************************************************//
    //set by ThreadPoolExecutor,call by future
    public final V getResult() throws ExecutionException {
        int code = state.get();
        if (code == State_Result) return result;
        if (code == State_Canceled) throw new CancellationException();
        if (code == State_ExecutionException) throw executionException;
        throw new InternalError();
    }

    //set by ThreadPoolExecutor
    public final void setResult(V result) {
        if (state.get() == State_Executing) {
            this.result = result;
            this.state.set(State_Result);
        }
    }

    //set by ThreadPoolExecutor
    public final void setExecutionException(Throwable e) {
        if (state.get() == State_Executing) {
            this.executionException = new ExecutionException(e);
            this.state.set(State_ExecutionException);
        }
    }

    //****************************************************************************************************************//
    //                                     4: execute call                                                            //
    //****************************************************************************************************************//
    public final void executeTask() {
        try {
            this.result = call.call();
            if (result == null && existDefaultResult) this.result = defaultResult;
            compareAndSetState(State_Executing, State_Result);
        } catch (Throwable e) {
            this.executionException = new ExecutionException(e);
            compareAndSetState(State_Executing, State_ExecutionException);
        }
    }
}
