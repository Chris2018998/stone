/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

import org.stone.beetp.pool.exception.TaskCancelledException;
import org.stone.beetp.pool.exception.TaskException;
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;

import java.util.concurrent.TimeUnit;

/**
 * An object handle interface,when submit a task to {@link TaskPool} success,pool return a handle,which can be used to
 * get call result of task or cancel execution of task.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskHandle<V> {

    /**
     * Query task state is whether in waiting for being executed
     *
     * @return true that in waiting
     */
    boolean isWaiting();

    /**
     * Query task state is whether in running
     *
     * @return true that in running
     */
    boolean isRunning();

    /**
     * Query task state is whether completed(success,failed,cancelled)
     *
     * @return true that task is completed
     */
    boolean isCompleted();

    /**
     * Query task state is whether cancelled
     *
     * @return true that task is cancelled
     */
    boolean isCancelled();

    /**
     * Query task state is whether success and a result can be retrieved by get method
     *
     * @return true that task is cancelled
     */
    boolean isSucceed();

    /**
     * Query task state is whether failed
     *
     * @return true that task execution failed
     */
    boolean isFailed();

    /**
     * Attempts to cancel task of this handle from pool.
     *
     * @param mayInterruptIfRunning is true then attempt to interrupt execution if exists blocking
     * @return a boolean value,true is that cancel success
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Get result of task call of this handle.
     *
     * @return an object type value as call result
     * @throws TaskCancelledException if task is cancelled
     * @throws TaskExecutionException if task is executed fail
     * @throws InterruptedException   if task execution is interrupted
     */
    V get() throws TaskException, InterruptedException;

    /**
     * Get result of task call of this handle.
     *
     * @param timeout is a max wait time for result of task call,a zero value is allowed
     * @param unit    is measure unit of timeout
     * @return an object type value as call result
     * @throws TaskCancelledException        if task is cancelled
     * @throws TaskExecutionException        if task is executed fail
     * @throws InterruptedException          if task execution is interrupted
     * @throws TaskResultGetTimeoutException if not get result when timeout reach
     */
    V get(long timeout, TimeUnit unit) throws TaskException, InterruptedException;
}
