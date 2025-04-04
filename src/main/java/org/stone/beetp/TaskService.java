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

import org.stone.beetp.pool.exception.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.stone.beetp.pool.PoolConstants.TASK_EXCEPTIONAL;
import static org.stone.beetp.pool.PoolConstants.TASK_SUCCEED;
import static org.stone.tools.BeanUtil.loadClass;

/**
 * Task service
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskService extends TaskServiceConfig {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private TaskPool pool;
    private boolean ready;
    private TaskPoolException cause;

    //***************************************************************************************************************//
    //                                             1:constructors(2)                                                 //
    //***************************************************************************************************************//
    public TaskService() {
    }

    public TaskService(TaskServiceConfig config) {
        try {
            config.copyTo(this);
            createPool(this);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createPool(TaskService service) throws Exception {
        Class<?> poolClass = loadClass(service.getPoolImplementClassName());
        if (!TaskPool.class.isAssignableFrom(poolClass))
            throw new TaskServiceConfigException("Invalid execution execution class name:" + service.getPoolImplementClassName());

        TaskPool pool = (TaskPool) poolClass.newInstance();
        pool.init(service);
        service.pool = pool;
        service.ready = true;
    }

    private static int checkParameters(Collection<? extends Task<?>> tasks, long timeout, TimeUnit unit) {
        if (tasks == null) throw new NullPointerException();
        if (unit == null) throw new NullPointerException();
        int totalSize = tasks.size();
        if (totalSize == 0) throw new IllegalArgumentException();
        return totalSize;
    }

    //***************************************************************************************************************//
    //                                        2: task submit methods(6)                                              //
    //***************************************************************************************************************//
    public <V> TaskHandle<V> submit(Task<V> task) throws TaskException, TaskPoolException {
        if (this.ready) return pool.submit(task);
        return createPoolByLock().submit(task);
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskAspect<V> callback) throws TaskException, TaskPoolException {
        if (this.ready) return pool.submit(task, callback);
        return createPoolByLock().submit(task, callback);
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> joinOperator) throws TaskException, TaskPoolException {
        if (this.ready) return pool.submit(task, joinOperator);
        return createPoolByLock().submit(task, joinOperator);
    }

    public <V> TaskHandle<V> submit(Task<V> task, TaskJoinOperator<V> joinOperator, TaskAspect<V> callback) throws TaskException, TaskPoolException {
        if (this.ready) return pool.submit(task, joinOperator, callback);
        return createPoolByLock().submit(task, joinOperator, callback);
    }

    public <V> TaskHandle<V> submit(TreeLayerTask<V> task) throws TaskException, TaskPoolException {
        if (this.ready) return pool.submit(task);
        return createPoolByLock().submit(task);
    }

    public <V> TaskHandle<V> submit(TreeLayerTask<V> task, TaskAspect<V> callback) throws TaskException, TaskPoolException {
        if (this.ready) return pool.submit(task, callback);
        return createPoolByLock().submit(task, callback);
    }

    //***************************************************************************************************************//
    //                                   3: task schedule(6)                                                          //
    //***************************************************************************************************************//
    public <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit) throws TaskException, TaskPoolException {
        if (this.ready) return pool.schedule(task, delay, unit);
        return createPoolByLock().schedule(task, delay, unit);
    }

    public <V> TaskScheduledHandle<V> schedule(Task<V> task, long delay, TimeUnit unit, TaskAspect<V> callback) throws TaskException, TaskPoolException {
        if (this.ready) return pool.schedule(task, delay, unit, callback);
        return createPoolByLock().schedule(task, delay, unit, callback);
    }

    public <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit) throws TaskException, TaskPoolException {
        if (this.ready) return pool.scheduleAtFixedRate(task, initialDelay, period, unit);
        return createPoolByLock().scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public <V> TaskScheduledHandle<V> scheduleAtFixedRate(Task<V> task, long initialDelay, long period, TimeUnit unit, TaskAspect<V> callback) throws TaskException, TaskPoolException {
        if (this.ready) return pool.scheduleAtFixedRate(task, initialDelay, period, unit, callback);
        return createPoolByLock().scheduleAtFixedRate(task, initialDelay, period, unit, callback);
    }

    public <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit) throws TaskException, TaskPoolException {
        if (this.ready) return pool.scheduleWithFixedDelay(task, initialDelay, delay, unit);
        return createPoolByLock().scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    public <V> TaskScheduledHandle<V> scheduleWithFixedDelay(Task<V> task, long initialDelay, long delay, TimeUnit unit, TaskAspect<V> callback) throws TaskException, TaskPoolException {
        if (this.ready) return pool.scheduleWithFixedDelay(task, initialDelay, delay, unit, callback);
        return createPoolByLock().scheduleWithFixedDelay(task, initialDelay, delay, unit, callback);
    }

    private TaskPool createPoolByLock() throws TaskPoolException {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!ready) {
                    cause = null;
                    createPool(this);
                }
            } catch (Throwable e) {
                if (e instanceof TaskPoolException)
                    cause = (TaskPoolException) e;
                else
                    cause = new TaskPoolException(e);
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            readLock.lock();
            readLock.unlock();
        }

        //read lock will reach
        if (cause != null) throw cause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                        4: execution clear(2)                                                       //
    //***************************************************************************************************************//
    public boolean clear(boolean mayInterruptIfRunning) throws TaskPoolException {
        try {
            return clear(mayInterruptIfRunning, null);
        } catch (TaskServiceConfigException e) {
            return false;
        }
    }

    public boolean clear(boolean mayInterruptIfRunning, TaskServiceConfig config) throws TaskPoolException, TaskServiceConfigException {
        this.checkPool();
        return pool.clear(mayInterruptIfRunning, config);
    }

    //***************************************************************************************************************//
    //                                        5: execution termination(4)                                                 //
    //***************************************************************************************************************//
    public boolean isTerminated() throws TaskPoolException {
        this.checkPool();
        return pool.isTerminated();
    }

    public boolean isTerminating() throws TaskPoolException {
        this.checkPool();
        return pool.isTerminating();
    }

    public int getRunningCount() throws TaskPoolException {
        this.checkPool();
        return pool.getRunningCount();
    }

    public long getCompletedCount() throws TaskPoolException {
        this.checkPool();
        return pool.getCompletedCount();
    }

    public void shutdown(boolean cancelRunningTask) throws TaskPoolException {
        this.checkPool();
        pool.terminate(cancelRunningTask);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException, TaskPoolException {
        this.checkPool();
        return pool.awaitTermination(timeout, unit);
    }

    private void checkPool() throws TaskPoolException {
        if (pool == null) throw new TaskPoolException("Task pool not be created");
    }

    //***************************************************************************************************************//
    //                                     6: Pool monitor(1)                                                        //
    //***************************************************************************************************************//
    public TaskPoolMonitorVo getPoolMonitorVo() throws TaskPoolException {
        this.checkPool();
        return pool.getPoolMonitorVo();
    }

    //***************************************************************************************************************//
    //                                        7: tasks invoke(4)                                                     //
    //***************************************************************************************************************//
    public <V> TaskHandle<V> invokeAny(Collection<? extends Task<V>> tasks) throws TaskException, TaskPoolException, InterruptedException {
        return invokeAny(tasks, 0L, TimeUnit.NANOSECONDS);
    }

    public <V> TaskHandle<V> invokeAny(Collection<? extends Task<V>> tasks, long timeout, TimeUnit unit) throws TaskException, TaskPoolException, InterruptedException {
        this.checkPool();
        //1: check parameters
        int taskSize = checkParameters(tasks, timeout, unit);
        //2: try to create execution if not ready
        if (!this.ready) pool = createPoolByLock();


        //3:task submission preparation
        TaskHandle<V> completedHandle;//contains a result
        AnyCallback<V> callback = new AnyCallback<>(taskSize);
        List<TaskHandle<V>> handleList = new ArrayList<>(taskSize);
        final boolean timed = timeout > 0L;
        final long deadline = timed ? System.nanoTime() + unit.toNanos(timeout) : 0L;

        try {
            //4:task submission
            for (Task<V> task : tasks) {
                //4.1:try to read out a handle from callback before submit a new task to execution
                completedHandle = callback.completedHandle;
                if (completedHandle != null) return completedHandle;
                //4.2:submit a task to execution
                handleList.add(pool.submit(task, callback));
                //4.3:timeout check
                if (timed && deadline - System.nanoTime() <= 0L) throw new TaskResultGetTimeoutException();
            }

            //5:spin to get a completed handle
            do {
                //5.1:try to read a handle
                completedHandle = callback.completedHandle;
                if (completedHandle != null) return completedHandle;
                //5.2:maybe all tasks failed or the last task cancelled by execution
                if (callback.doneCount.get() == taskSize) break;

                //5.3:parking(ThreadSpinParker is a better choice?)
                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime <= 0L) throw new TaskResultGetTimeoutException();
                    LockSupport.parkNanos(parkTime);
                } else {
                    LockSupport.park();
                }

                //5.4:park interruption check
                if (Thread.interrupted()) throw new InterruptedException();
            } while (true);

            //6:if execution exception filled in callback object,then throw it
            if (callback.failCause != null) throw callback.failCause;
            throw new TaskExecutionException();
        } finally {
            //7:cancel not done tasks
            for (TaskHandle<V> handle : handleList)
                if (!handle.isCompleted()) handle.cancel(true);
        }
    }

    public <V> List<TaskHandle<V>> invokeAll(Collection<? extends Task<V>> tasks) throws TaskException, TaskPoolException, InterruptedException {
        return invokeAll(tasks, 0L, TimeUnit.NANOSECONDS);
    }

    public <V> List<TaskHandle<V>> invokeAll(Collection<? extends Task<V>> tasks, long timeout, TimeUnit unit) throws TaskException, TaskPoolException, InterruptedException {
        this.checkPool();
        //1: check parameters
        int taskSize = checkParameters(tasks, timeout, unit);
        //2: try to create execution if not ready
        if (!this.ready) pool = createPoolByLock();

        //3:task submission preparation
        AllCallback<V> callback = new AllCallback<>(taskSize);
        List<TaskHandle<V>> handleList = new ArrayList<>(taskSize);//submitted list
        boolean timed = timeout > 0L;
        long deadline = timed ? System.nanoTime() + unit.toNanos(timeout) : 0L;
        boolean allDone = false;

        try {
            //4:task submission
            for (Task<V> task : tasks) {
                handleList.add(pool.submit(task, callback));
                if (timed && deadline - System.nanoTime() <= 0L) return handleList;
            }

            //5: spin for all tasks done
            do {
                //5.1:if completed count equals task size,then exit spin
                if (callback.doneCount.get() == taskSize) {
                    allDone = true;
                    break;
                }

                //5.2:parking call thread
                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime <= 0L) break;//timeout,then break
                    LockSupport.parkNanos(parkTime);
                } else {
                    LockSupport.park();
                }

                //5.3: park interruption check
                if (Thread.interrupted()) throw new InterruptedException();
            } while (true);

            return handleList;
        } finally {
            if (!allDone) {//timeout or interrupted
                for (TaskHandle<V> handle : handleList)
                    if (!handle.isCompleted()) handle.cancel(true);
            }
        }
    }

    //***************************************************************************************************************//
    //                             8: callback impl(2)(result collector and wakeup call thread)                      //
    //***************************************************************************************************************//
    private static final class AnyCallback<V> implements TaskAspect<V> {
        private final int taskSize;
        private final Thread callThread;
        private final AtomicInteger doneCount;
        private volatile TaskHandle<V> completedHandle;//we don't care who arrive firstly
        private volatile TaskExecutionException failCause;

        AnyCallback(int taskTotalSize) {
            this.taskSize = taskTotalSize;
            this.callThread = Thread.currentThread();
            this.doneCount = new AtomicInteger(0);
        }

        public void beforeCall(TaskHandle<V> handle) {
        }

        //1:task completed 2:execute exception 3:task cancelled by execution
        public void afterCall(boolean isSuccess, Object resultObject, TaskHandle<V> handle) {
            boolean hasWakeup = false;
            try {
                final Object targetState = isSuccess ? TASK_SUCCEED : TASK_EXCEPTIONAL;
                if (TASK_EXCEPTIONAL == targetState && resultObject instanceof TaskExecutionException)
                    this.failCause = (TaskExecutionException) resultObject;
                else if (TASK_SUCCEED == targetState) {
                    this.completedHandle = handle;
                    LockSupport.unpark(callThread);
                    hasWakeup = true;
                }
            } finally {
                if (doneCount.incrementAndGet() == taskSize && !hasWakeup) LockSupport.unpark(callThread);
            }
        }
    }

    private static final class AllCallback<V> implements TaskAspect<V> {
        private final int taskSize;
        private final Thread callThread;
        private final AtomicInteger doneCount;

        AllCallback(int taskSize) {
            this.taskSize = taskSize;
            this.callThread = Thread.currentThread();
            this.doneCount = new AtomicInteger(0);
        }

        public void beforeCall(TaskHandle<V> handle) {
        }

        public void afterCall(boolean isSuccess, Object resultObject, TaskHandle<V> handle) {
            //final Object targetState = isSuccess ? TASK_SUCCEED : TASK_FAILED;

            if (this.doneCount.incrementAndGet() == taskSize)
                LockSupport.unpark(callThread);
        }
    }
}
