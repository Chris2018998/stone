/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.stone.study.queue;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * RunnableFuture implementation
 *
 * @author Chris Liao
 */
public class MyFutureTask<V> implements RunnableFuture {
    //task status
    private static final int TASK_NEW = 0;
    private static final int TASK_RUNNING = 1;
    private static final int TASK_COMPLETED = 2;
    private static final int TASK_EXCEPTIONAL = 3;
    private static final int TASK_CANCELLED = 4;

    //waiter status
    private static final int WAITING = 0;//LockSupport.park()
    private static final int WAKEUP = 1;//LockSupport.unpark()
    private static final int FAILED = 2;//interrupt or timeout

    private static final AtomicIntegerFieldUpdater<Waiter> updater = AtomicIntegerFieldUpdater
            .newUpdater(Waiter.class, "state");

    private Callable<V> callable;
    private AtomicInteger taskState;
    private volatile Thread taskThread;
    private AtomicBoolean requestCancel;
    private ConcurrentLinkedQueue<Waiter> waitQueue;

    private V result;
    private ExecutionException exception;

    public MyFutureTask(Callable<V> callable) {
        if (callable == null) throw new NullPointerException();

        this.callable = callable;
        this.taskState = new AtomicInteger(TASK_NEW);
        this.waitQueue = new ConcurrentLinkedQueue();
        this.requestCancel = new AtomicBoolean(false);
    }

    /**
     * @return {@code true} if this task was cancelled before it completed
     */
    public boolean isCancelled() {
        return taskState.get() == TASK_CANCELLED;
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
        return taskState.get() > TASK_RUNNING;
    }

    /**
     * Sets this Future to the result of its computation
     * unless it has been cancelled.
     */
    public void run() {
        if (taskState.compareAndSet(TASK_NEW, TASK_RUNNING)) {
            try {
                taskThread = Thread.currentThread();
                V tempValue = callable.call();
                if (taskThread.isInterrupted() && requestCancel.get()) {
                    taskState.set(TASK_CANCELLED);
                } else {
                    result = tempValue;
                    taskState.set(TASK_COMPLETED);
                }
            } catch (Throwable e) {
                exception = new ExecutionException(e);
                taskState.set(TASK_EXCEPTIONAL);
            } finally {
                wakeupWaiters();
            }
        }
    }

    //wakeup all waiters to take result
    private void wakeupWaiters() {
        Waiter waiter;
        while ((waiter = waitQueue.poll()) != null) {
            if (updater.compareAndSet(waiter, WAITING, WAKEUP))
                LockSupport.unpark(waiter.thread);
        }
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
        if (taskState.compareAndSet(TASK_NEW, TASK_CANCELLED)) {
            return true;
        } else if (taskState.get() == TASK_RUNNING && mayInterruptIfRunning && taskThread != null) {
            Thread.State state = taskThread.getState();
            if ((state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) && requestCancel.compareAndSet(false, true)) {
                taskThread.interrupt();
                return taskState.get() == TASK_CANCELLED;
            }
        }
        return false;
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     */
    public V get() throws InterruptedException, ExecutionException {
        int state = taskState.get();
        if (state == TASK_COMPLETED) return result;
        if (state == TASK_EXCEPTIONAL) throw exception;
        if (state == TASK_CANCELLED) throw new CancellationException();

        //task is New or Running
        Waiter waiter = new Waiter();
        waitQueue.add(waiter);
        LockSupport.park();
        if (waiter.state == WAITING && updater.compareAndSet(waiter, WAITING, FAILED)) {
            waitQueue.remove(waiter);
            if (waiter.thread.isInterrupted()) {
                throw new InterruptedException();
            }
        }

        state = taskState.get();
        if (state == TASK_COMPLETED) return result;
        if (state == TASK_EXCEPTIONAL) throw exception;
        if (state == TASK_CANCELLED) throw new CancellationException();

        throw new ExecutionException("unkown error", null);
    }

    /**
     * Waits if necessary for at most the given parkTime for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum parkTime to wait
     * @param unit    the parkTime unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     * @throws TimeoutException      if the wait timed out
     */
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        int state = taskState.get();
        if (state == TASK_COMPLETED) return result;
        if (state == TASK_EXCEPTIONAL) throw exception;
        if (state == TASK_CANCELLED) throw new CancellationException();

        //task is New or Running
        Waiter waiter = new Waiter();
        waitQueue.add(waiter);
        LockSupport.park(unit.toNanos(timeout));
        if (waiter.state == WAITING && updater.compareAndSet(waiter, WAITING, FAILED)) {
            waitQueue.remove(waiter);
            if (waiter.thread.isInterrupted()) {
                throw new InterruptedException();
            } else {
                throw new TimeoutException();
            }
        }

        state = taskState.get();
        if (state == TASK_COMPLETED) return result;
        if (state == TASK_EXCEPTIONAL) throw exception;
        if (state == TASK_CANCELLED) throw new CancellationException();

        //task is New or Running
        throw new TimeoutException();
    }


    private static final class Waiter {
        volatile int state;
        Thread thread = Thread.currentThread();
    }
}
