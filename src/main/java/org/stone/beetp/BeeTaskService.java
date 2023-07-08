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

import org.stone.beetp.pool.TaskPoolStaticUtil;
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.beetp.pool.exception.TaskResultGetTimeoutException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Task service
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeTaskService extends BeeTaskServiceConfig {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private BeeTaskPool pool;
    private boolean ready;
    private BeeTaskPoolException cause;

    //***************************************************************************************************************//
    //                                             1:constructors(2)                                                 //
    //***************************************************************************************************************//
    public BeeTaskService() {
    }

    public BeeTaskService(BeeTaskServiceConfig config) {
        try {
            config.copyTo(this);
            createPool(this);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createPool(BeeTaskService service) throws Exception {
        Class<?> poolClass = Class.forName(service.getPoolImplementClassName());
        if (!BeeTaskPool.class.isAssignableFrom(poolClass))
            throw new BeeTaskServiceConfigException("Invalid pool implement class name:" + service.getPoolImplementClassName());

        BeeTaskPool pool = (BeeTaskPool) poolClass.newInstance();
        pool.init(service);
        service.pool = pool;
        service.ready = true;
    }

    //***************************************************************************************************************//
    //                                        2: task submit methods(2)                                              //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.submit(task);
        return createPoolByLock().submit(task);
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.submit(task, callback);
        return createPoolByLock().submit(task, callback);
    }

    //***************************************************************************************************************//
    //                              3: task schedule(6)                                                              //
    //***************************************************************************************************************//
    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.schedule(task, delay, unit);
        return createPoolByLock().schedule(task, delay, unit);
    }

    public BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.schedule(task, delay, unit, callback);
        return createPoolByLock().schedule(task, delay, unit, callback);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.scheduleAtFixedRate(task, initialDelay, period, unit);
        return createPoolByLock().scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.scheduleAtFixedRate(task, initialDelay, period, unit, callback);
        return createPoolByLock().scheduleAtFixedRate(task, initialDelay, period, unit, callback);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.scheduleWithFixedDelay(task, initialDelay, delay, unit);
        return createPoolByLock().scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    public BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.scheduleWithFixedDelay(task, initialDelay, delay, unit, callback);
        return createPoolByLock().scheduleWithFixedDelay(task, initialDelay, delay, unit, callback);
    }

    private BeeTaskPool createPoolByLock() throws BeeTaskPoolException {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!ready) {
                    cause = null;
                    createPool(this);
                }
            } catch (Throwable e) {
                if (e instanceof BeeTaskPoolException)
                    cause = (BeeTaskPoolException) e;
                else
                    cause = new BeeTaskPoolException(e);
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
    //                                        4: pool clear(2)                                                       //
    //***************************************************************************************************************//
    public boolean clear(boolean mayInterruptIfRunning) throws BeeTaskPoolException {
        try {
            return clear(mayInterruptIfRunning, null);
        } catch (BeeTaskServiceConfigException e) {
            return false;
        }
    }

    public boolean clear(boolean mayInterruptIfRunning, BeeTaskServiceConfig config) throws BeeTaskPoolException, BeeTaskServiceConfigException {
        if (pool == null) throw new BeeTaskPoolException("Task pool not be initialized");
        return pool.clear(mayInterruptIfRunning, config);
    }

    //***************************************************************************************************************//
    //                                        5: pool termination(4)                                                 //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return pool == null || pool.isTerminated();
    }

    public boolean isTerminating() {
        return pool == null || pool.isTerminating();
    }

    public void terminate(boolean cancelRunningTask) throws BeeTaskPoolException {
        if (pool == null) throw new BeeTaskPoolException("Task pool not be initialized");
        pool.terminate(cancelRunningTask);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException, BeeTaskPoolException {
        if (pool == null) throw new BeeTaskPoolException("Task pool not be initialized");
        return pool.awaitTermination(timeout, unit);
    }

    //***************************************************************************************************************//
    //                                     6: Pool monitor(1)                                                        //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() throws BeeTaskPoolException {
        if (pool == null) throw new BeeTaskPoolException("Task pool not be initialized");
        return pool.getPoolMonitorVo();
    }

    //***************************************************************************************************************//
    //                                        7: tasks invoke(4)                                                     //
    //***************************************************************************************************************//
    public BeeTaskHandle invokeAny(Collection<? extends BeeTask> tasks) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        return invokeAny(tasks, 0, TimeUnit.NANOSECONDS);
    }

    public BeeTaskHandle invokeAny(Collection<? extends BeeTask> tasks, long timeout, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        //1:parameters check
        if (tasks == null) throw new NullPointerException();
        int taskSize = tasks.size();
        if (taskSize == 0) throw new IllegalArgumentException();
        if (unit == null) throw new NullPointerException();

        //2:try to create pool if not ready
        if (!this.ready) pool = createPoolByLock();

        //3:task submission preparation
        BeeTaskHandle completedHandle;//contains a result
        AnyCallback callback = new AnyCallback(taskSize);
        List<BeeTaskHandle> handleList = new ArrayList<>(taskSize);
        final boolean timed = timeout > 0;
        final long deadline = timed ? System.nanoTime() + unit.toNanos(timeout) : 0;

        try {
            //4:task submission
            for (BeeTask task : tasks) {
                //4.1:try to read out a handle from callback before submit a new task to pool
                completedHandle = callback.completedHandle;
                if (completedHandle != null) return completedHandle;
                //4.2:submit a task to pool
                handleList.add(pool.submit(task, callback));
                //4.3:timeout check
                if (timed && deadline - System.nanoTime() <= 0) throw new TaskResultGetTimeoutException();
            }

            //5:spin to get a completed handle
            do {
                //5.1:try to read a handle
                completedHandle = callback.completedHandle;
                if (completedHandle != null) return completedHandle;
                //5.2:maybe all tasks failed or the last task cancelled by pool
                if (callback.doneCount.get() == taskSize) break;

                //5.3:parking(ThreadSpinPark is a better choice?)
                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime <= 0) throw new TaskResultGetTimeoutException();
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
            for (BeeTaskHandle handle : handleList)
                if (!handle.isDone()) handle.cancel(true);
        }
    }

    public List<BeeTaskHandle> invokeAll(Collection<? extends BeeTask> tasks) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        return invokeAll(tasks, 0, TimeUnit.NANOSECONDS);
    }

    public List<BeeTaskHandle> invokeAll(Collection<? extends BeeTask> tasks, long timeout, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        //1: parameters check
        if (tasks == null) throw new NullPointerException();
        int totalSize = tasks.size();
        if (totalSize == 0) throw new IllegalArgumentException();
        if (unit == null) throw new NullPointerException();

        //2: try to create pool if not ready
        if (!this.ready) pool = createPoolByLock();

        //3:task submission preparation
        AllCallback callback = new AllCallback(totalSize);
        List<BeeTaskHandle> handleList = new ArrayList<>(totalSize);//submitted list
        boolean timed = timeout > 0;
        long deadline = timed ? System.nanoTime() + unit.toNanos(timeout) : 0;
        boolean allDone = false;

        try {
            //4:task submission
            for (BeeTask task : tasks) {
                handleList.add(pool.submit(task, callback));
                if (timed && deadline - System.nanoTime() <= 0) return handleList;
            }

            //5: spin for all tasks done
            do {
                //5.1:if completed count equals task size,then exit spin
                if (callback.doneCount.get() == totalSize) {
                    allDone = true;
                    break;
                }

                //5.2:parking call thread
                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime <= 0) break;//timeout,then break
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
                for (BeeTaskHandle handle : handleList)
                    if (!handle.isDone()) handle.cancel(true);
            }
        }
    }

    //***************************************************************************************************************//
    //                             8: callback impl(2)(result collector and wakeup call thread)                      //
    //***************************************************************************************************************//
    private static final class AnyCallback implements BeeTaskCallback {
        private final int taskSize;
        private final Thread callThread;
        private final AtomicInteger doneCount;
        private volatile BeeTaskHandle completedHandle;//we don't care who arrive firstly
        private volatile TaskExecutionException failCause;

        AnyCallback(int taskTotalSize) {
            this.taskSize = taskTotalSize;
            this.callThread = Thread.currentThread();
            this.doneCount = new AtomicInteger(0);
        }

        public void beforeCall(BeeTaskHandle handle) {
        }

        //1:task completed 2:execute exception 3:task cancelled by pool
        public void onCallDone(int doneCode, Object doneResp, BeeTaskHandle handle) {
            boolean hasWakeup = false;
            try {
                if (TaskPoolStaticUtil.TASK_EXCEPTION == doneCode && doneResp instanceof TaskExecutionException)
                    this.failCause = (TaskExecutionException) doneResp;
                else if (TaskPoolStaticUtil.TASK_RESULT == doneCode) {
                    this.completedHandle = handle;
                    LockSupport.unpark(callThread);
                    hasWakeup = true;
                }
            } finally {
                if (doneCount.incrementAndGet() == taskSize && !hasWakeup) LockSupport.unpark(callThread);
            }
        }
    }

    private static final class AllCallback implements BeeTaskCallback {
        private final int taskSize;
        private final Thread callThread;
        private final AtomicInteger doneCount;

        AllCallback(int taskSize) {
            this.taskSize = taskSize;
            this.callThread = Thread.currentThread();
            this.doneCount = new AtomicInteger(0);
        }

        public void beforeCall(BeeTaskHandle handle) {
        }

        public void onCallDone(int doneCode, Object doneResp, BeeTaskHandle handle) {
            if (this.doneCount.incrementAndGet() == taskSize)
                LockSupport.unpark(callThread);
        }
    }
}
