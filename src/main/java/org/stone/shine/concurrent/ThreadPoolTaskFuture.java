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

import org.stone.shine.synchronizer.ThreadWaitConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future Impl by wait pool(which inside ThreadPoolExecutor)
 * <p>
 * TaskHolder contains two attributes:taskFuture,Callable
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ThreadPoolTaskFuture<V> implements Future<V> {
    private final ThreadPoolTask<V> task;
    private final ThreadPoolExecutor executor;

    ThreadPoolTaskFuture(ThreadPoolTask<V> task, ThreadPoolExecutor executor) {
        this.task = task;
        this.executor = executor;
    }

    /**
     * Returns {@code true} if this task completed.
     * <p>
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    public boolean isDone() {
        return task.isDone();
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    public boolean isCancelled() {
        return task.isCancelled();
    }

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return executor.cancelTask(task, mayInterruptIfRunning);
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws InterruptedException,                      which be thrown from wait pool,and just thrown once per waiter;if the current thread was interrupted while waiting
     * @throws ExecutionException,                        which from executor,if the computation threw an exception
     * @throws java.util.concurrent.CancellationException if the computation was cancelled
     */
    public V get() throws InterruptedException, ExecutionException {
        //step1:read result by state code
        if (task.isDone()) return task.getResult();

        //step2:wait result
        ThreadWaitConfig config = new ThreadWaitConfig();
        config.setNodeType(task.getTaskId());
        executor.waitCallResult(config);

        //step3:read result by state code
        return task.getResult();
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the computed result
     * @throws java.util.concurrent.CancellationException if the computation was cancelled
     * @throws InterruptedException                       which be thrown from wait pool,and just thrown once per waiter;if the current thread was interrupted while waiting
     * @throws TimeoutException                           if the wait timed out
     * @throws ExecutionException                         if the computation threw an  exception
     */
    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
        //step1:read result by state code
        if (task.isDone()) return task.getResult();

        //step2:wait result
        ThreadWaitConfig config = new ThreadWaitConfig(timeout, unit);
        config.setNodeType(task.getTaskId());
        executor.waitCallResult(config);
        if (config.getThreadParkSupport().isTimeout()) throw new TimeoutException();

        //step3:read result by state code
        return task.getResult();
    }
}
