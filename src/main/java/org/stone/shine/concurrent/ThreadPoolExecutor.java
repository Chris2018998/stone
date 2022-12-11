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
import org.stone.shine.synchronizer.base.StateWaitPool;
import org.stone.shine.synchronizer.base.validator.AnyValidator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executor Impl by wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ThreadPoolExecutor<E> implements ExecutorService {
    //allow max size in queue
    private int taskMaxSize;
    //number of task in queue(not running + running)
    private AtomicInteger taskCountInQueue = new AtomicInteger(0);
    //number of task in running
    private AtomicInteger taskCountInRunning = new AtomicInteger(0);
    //number of competed size(done+cancel)
    private AtomicInteger taskCountInCompleted = new AtomicInteger(0);
    //number of canceled task
    private AtomicInteger taskCountInCanceled = new AtomicInteger(0);
    //task queue
    private ConcurrentLinkedQueue taskQueue = new ConcurrentLinkedQueue();

    //allow max size to work
    private int workerMaxSize;
    //worker wait time for task
    private volatile long keepAliveTime;
    //worker thread factory
    private volatile ThreadFactory workerThreadFactory;
    //Handler called when saturated or shutdown in execute.
    private volatile RejectedExecutionHandler handler;
    //for Future wait and awaitTermination,Worker thread
    private StateWaitPool waitPool = new StateWaitPool(new AnyValidator());


    //****************************************************************************************************************//
    //                                          1: constructor(2)                                                     //
    //****************************************************************************************************************//
    public ThreadPoolExecutor(int taskMaxSize, int workerMaxSize, long keepAliveTime, TimeUnit unit) {
        this(taskMaxSize, workerMaxSize, keepAliveTime, unit, Executors.defaultThreadFactory(), new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    }

    public ThreadPoolExecutor(int taskMaxSize, int workerMaxSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        this.taskMaxSize = taskMaxSize;
        this.workerMaxSize = workerMaxSize;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.workerThreadFactory = threadFactory;
        this.handler = handler;
    }

    //****************************************************************************************************************//
    //                                          2: Executor public methods                                            //
    //****************************************************************************************************************//

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException if a security manager exists and
     *                           shutting down this ExecutorService may manipulate
     *                           threads that the caller is not permitted to modify
     *                           because it does not hold {@link
     *                           java.lang.RuntimePermission}{@code ("modifyThread")},
     *                           or the security manager's {@code checkAccess} method
     *                           denies access.
     */
    public void shutdown() {
        //@todo
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution
     * @throws SecurityException if a security manager exists and
     *                           shutting down this ExecutorService may manipulate
     *                           threads that the caller is not permitted to modify
     *                           because it does not hold {@link
     *                           java.lang.RuntimePermission}{@code ("modifyThread")},
     *                           or the security manager's {@code checkAccess} method
     *                           denies access.
     */
    public List<Runnable> shutdownNow() {
        //@todo
        return null;
    }

    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    public boolean isShutdown() {
        //@todo
        return false;
    }

    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    public boolean isTerminated() {
        //@todo
        return false;
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     * {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        //@todo
        return false;
    }

    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the {@code Executor} implementation.
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     *                                    accepted for execution
     * @throws NullPointerException       if command is null
     */
    public void execute(Runnable command) {
        //@todo
    }

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @param <T>  the type of the task's result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    public <T> Future<T> submit(Callable<T> task) {
        //@todo
        return null;
    }

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task   the task to submit
     * @param result the result to return
     * @param <T>    the type of the result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    public <T> Future<T> submit(Runnable task, T result) {
        //@todo
        return null;
    }

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    public Future<?> submit(Runnable task) {
        //@todo
        return null;
    }

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T>   the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     * sequential order as produced by the iterator for the
     * given task list, each of which has completed
     * @throws InterruptedException       if interrupted while waiting, in
     *                                    which case unfinished tasks are cancelled
     * @throws NullPointerException       if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *                                    scheduled for execution
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        //@todo
        return null;
    }

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks   the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @param <T>     the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     * sequential order as produced by the iterator for the
     * given task list. If the operation did not time out,
     * each task will have completed. If it did time out, some
     * of these tasks will not have completed.
     * @throws InterruptedException       if interrupted while waiting, in
     *                                    which case unfinished tasks are cancelled
     * @throws NullPointerException       if tasks, any of its elements, or
     *                                    unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *                                    for execution
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        //@todo
        return null;
    }

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do. Upon normal or exceptional return,
     * tasks that have not completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T>   the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException       if interrupted while waiting
     * @throws NullPointerException       if tasks or any element task
     *                                    subject to execution is {@code null}
     * @throws IllegalArgumentException   if tasks is empty
     * @throws ExecutionException         if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *                                    for execution
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        //@todo
        return null;
    }

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks   the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @param <T>     the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException       if interrupted while waiting
     * @throws NullPointerException       if tasks, or unit, or any element
     *                                    task subject to execution is {@code null}
     * @throws TimeoutException           if the given timeout elapses before
     *                                    any task successfully completes
     * @throws ExecutionException         if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *                                    for execution
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //@todo
        return null;
    }

    //****************************************************************************************************************//
    //                                          3:ThreadPoolExecutor inner methods                                    //
    //****************************************************************************************************************//
    boolean cancelTask(Object taskHolder, boolean mayInterruptIfRunning) {
        return false;
        //@todo to be dev
    }

    void waitCallResult(ThreadWaitConfig config) throws InterruptedException {
        waitPool.doWait(config);
    }

    //****************************************************************************************************************//
    //                                          4:ThreadPoolExecutor inner classes                                    //
    //****************************************************************************************************************//

}
