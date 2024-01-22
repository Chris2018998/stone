/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import org.stone.beetp.pool.exception.TaskCancelledException;
import org.stone.beetp.pool.exception.TaskException;
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;

import java.util.concurrent.TimeUnit;

/**
 * A object handle interface for a submitted task,after task submission to a pool,
 * an implementation instance of this interface will be generated and return to submitter
 * <p>
 * A task handle provides three kind of methods:
 * 1: task state query method,@see {@link #getState}
 * 2: task cancellation method,@see {@link #cancel}
 * 3: result getting methods,@see {@link #get}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskHandle<T> {

    /**
     * return present state of a task;state static definition,@see{@link TaskStates}
     *
     * @return task state
     */
    int getState();

    /**
     * try to cancel this task from pool
     *
     * @param mayInterruptIfRunning true,if task has been in blocking,then interrupt it
     * @return true means cancel success,otherwise,task has been completed
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * retrieves task computed result,some keys point are below
     * 1: returns result or throws exception immediately when task call is in completed state
     * 2: if task  not in completed state,then blocking util completion
     * 3: supports interruption while blocking
     *
     * @return the computed result
     * @throws TaskCancelledException if the computation was cancelled
     * @throws TaskExecutionException if the computation threw an exception
     * @throws InterruptedException   if the current thread was interrupted while waiting
     */
    T get() throws TaskException, InterruptedException;

    /**
     * retrieves task computed result,some keys point are below
     * 1: returns result or throws exception immediately when task call is in completed state
     * 2: if task not in completed state,then blocking util completion
     * 3: supports timed waiting to get computed result
     * 4: supports interruption while blocking
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the computed result
     * @throws TaskCancelledException        if the computation was cancelled
     * @throws TaskExecutionException        if the computation threw an exception
     * @throws InterruptedException          if the current thread was interrupted while waiting
     * @throws TaskResultGetTimeoutException if the wait timed out
     */
    T get(long timeout, TimeUnit unit) throws TaskException, InterruptedException;
}
