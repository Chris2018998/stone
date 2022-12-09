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

import java.util.concurrent.*;

/**
 * CompletionService Impl by wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ExecutorCompletionService<V> implements CompletionService<V> {

    private ExecutorService executorService;

    public ExecutorCompletionService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task.  Upon completion,
     * this task may be taken or polled.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    public Future<V> submit(Callable<V> task) {
        return executorService.submit(task);
    }

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task.  Upon completion, this task may be
     * taken or polled.
     *
     * @param task   the task to submit
     * @param result the result to return upon successful completion
     * @return a Future representing pending completion of the task,
     * and whose {@code get()} method will return the given
     * result value upon completion
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    public Future<V> submit(Runnable task, V result) {
        return executorService.submit(task, result);
    }

    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if none are yet present.
     *
     * @return the Future representing the next completed task
     * @throws InterruptedException if interrupted while waiting
     */
    public Future<V> take() throws InterruptedException {
        return null;
    }

    /**
     * Retrieves and removes the Future representing the next
     * completed task, or {@code null} if none are present.
     *
     * @return the Future representing the next completed task, or
     * {@code null} if none are present
     */
    public Future<V> poll() {
        return null;
    }

    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if necessary up to the specified wait
     * time if none are yet present.
     *
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     * @return the Future representing the next completed task or
     * {@code null} if the specified waiting time elapses
     * before one is present
     * @throws InterruptedException if interrupted while waiting
     */
    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }
}
