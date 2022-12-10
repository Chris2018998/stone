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

import java.util.UUID;
import java.util.concurrent.*;

/**
 * Future Impl by wait pool(which inside ThreadPoolExecutor)
 * <p>
 * TaskHolder contains two attributes:taskFuture,Callable
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ThreadPoolExecutorFuture<V> implements Future<V> {
    //task in executing
    private static final int State_Doing = 1;
    //task done
    private static final int State_Result = 2;
    //task canceled
    private static final int State_Canceled = 3;
    //task execution exception
    private static final int State_ExecutionException = 4;

    private final UUID futureId;
    //private final TaskHolder taskHolder;@todo to be dev
    private final ThreadPoolExecutor executor;
    //future state
    private volatile int state;

    private V result;
    private ExecutionException executionException;

    ThreadPoolExecutorFuture(ThreadPoolExecutor executor) {
        this.executor = executor;
        this.state = State_Doing;
        this.futureId = UUID.randomUUID();
        //this.taskHolder =taskHolder;//@todo to be dev
    }

    //****************************************************************************************************************//
    //                                          2: Future methods                                                     //
    //****************************************************************************************************************//

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
        return state >= State_Result;
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    public boolean isCancelled() {
        return state == State_Canceled;
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
        //@todo if Interrupt success,the task executor will abandon task
        return false;
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
        int code = state;
        if (code != State_Doing) return getResult(code);

        //step2:wait result
        ThreadWaitConfig config = new ThreadWaitConfig();
        config.setNodeType(futureId);
        //@todo wait int pool(InterruptedException can be thrown from wait pool)

        //step3:read result by state code
        return getResult(state);
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
        int code = state;
        if (code != State_Doing) return getResult(code);

        //step2:wait result
        ThreadWaitConfig config = new ThreadWaitConfig(timeout, unit);
        config.setNodeType(futureId);
        //@todo wait int pool(InterruptedException can be thrown from wait pool)
        if (config.getThreadParkSupport().isTimeout()) throw new TimeoutException();

        //step3:read result by state code
        return getResult(state);
    }

    //****************************************************************************************************************//
    //                                          3: result get/set methods                                             //
    //****************************************************************************************************************//
    //call by ThreadPoolExecutor to get wakeup key after task over
    UUID getFutureId() {
        return futureId;
    }

    //set by ThreadPoolExecutor
    void setResult(V result) {
        if (state == State_Doing) {
            this.result = result;
            this.state = State_Result;
        }
    }

    //set by ThreadPoolExecutor
    void setExecutionException(Throwable e) {
        if (state == State_Doing) {
            this.executionException = new ExecutionException(e);
            this.state = State_ExecutionException;
        }
    }

    //call in get methods
    private V getResult(int code) throws ExecutionException {
        if (code == State_Result) return result;
        if (code == State_Canceled) throw new CancellationException();
        if (code == State_ExecutionException) throw executionException;
        throw new InternalError();
    }
}
