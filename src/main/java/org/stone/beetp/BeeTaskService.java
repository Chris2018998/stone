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

import org.stone.beetp.pool.exception.TaskExecutionException;

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
    private Exception cause;

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
    //                                        2: task submit methods(4)                                              //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws Exception {
        if (this.ready) return pool.submit(task);
        return createPoolByLock().submit(task);
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws Exception {
        if (this.ready) return pool.submit(task, callback);
        return createPoolByLock().submit(task, callback);
    }

    public BeeTaskHandle submit(BeeTaskConfig taskConfig) throws Exception {
        if (this.ready) return pool.submit(taskConfig);
        return createPoolByLock().submit(taskConfig);
    }

    private BeeTaskPool createPoolByLock() throws Exception {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!ready) {
                    cause = null;
                    createPool(this);
                }
            } catch (Exception e) {
                cause = e;
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
    //                                        3: pool terminate methods(4)                                           //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return pool == null || pool.isTerminated();
    }

    public boolean isTerminating() {
        return pool == null || pool.isTerminating();
    }

    public BeeTaskPoolMonitorVo getPoolMonitorVo() throws Exception {
        if (pool == null) throw new BeeTaskPoolException("Task pool not initialized");
        return pool.getPoolMonitorVo();
    }

    public void terminate(boolean cancelRunningTask) throws BeeTaskPoolException {
        if (pool != null) pool.terminate(cancelRunningTask);
    }

    //***************************************************************************************************************//
    //                                        4: task list methods(4)                                                //
    //***************************************************************************************************************//
    public BeeTaskHandle invokeAny(Collection<? extends BeeTask> tasks) throws Exception {
        return invokeAny(tasks, 0, TimeUnit.NANOSECONDS);
    }

    public BeeTaskHandle invokeAny(Collection<? extends BeeTask> tasks, long timeout, TimeUnit unit) throws Exception {
        if (tasks == null) throw new NullPointerException();
        boolean timed = timeout > 0;
        long deadline = System.nanoTime() + (timed ? unit.toNanos(timeout) : 0);
        AnyCallback callback = new AnyCallback();

        BeeTaskHandle completedHandle = null;
        List<BeeTaskHandle> handleList = new ArrayList(tasks.size());
        boolean done = false;
        try {
            for (BeeTask task : tasks) {
                if ((completedHandle = callback.handle) != null) break;
                handleList.add(pool.submit(task, callback));
            }

            do {
                if ((completedHandle = callback.handle) != null) break;
                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime <= 0) throw new TaskExecutionException("Timeout");
                    LockSupport.parkNanos(parkTime);
                } else {
                    LockSupport.park();
                }
            } while (true);
            done = true;
            return completedHandle;
        } catch (Throwable e) {
            throw new TaskExecutionException(e.getCause());
        } finally {
            if (!done) for (BeeTaskHandle handle : handleList) handle.cancel(true);
        }
    }

    public List<BeeTaskHandle> invokeAll(Collection<? extends BeeTask> tasks) throws Exception {
        return invokeAll(tasks, 0, TimeUnit.NANOSECONDS);
    }

    public List<BeeTaskHandle> invokeAll(Collection<? extends BeeTask> tasks, long timeout, TimeUnit unit) throws Exception {
        if (tasks == null) throw new NullPointerException();
        if (unit == null) throw new NullPointerException();

        boolean timed = timeout > 0;
        long deadline = System.nanoTime() + (timed ? unit.toNanos(timeout) : 0);
        int taskSize = tasks.size();
        AllCallback callback = new AllCallback(taskSize);
        List<BeeTaskHandle> handleList = new ArrayList(taskSize);
        boolean done = false;
        try {
            for (BeeTask task : tasks) handleList.add(pool.submit(task, callback));

            do {
                if (callback.completedSize.get() == taskSize) break;
                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime <= 0) throw new TaskExecutionException("Timeout");
                    LockSupport.parkNanos(parkTime);
                } else {
                    LockSupport.park();
                }
            } while (true);
            done = true;
            return handleList;
        } catch (Throwable e) {
            throw new TaskExecutionException(e.getCause());
        } finally {
            if (!done) for (BeeTaskHandle handle : handleList) handle.cancel(true);
        }
    }

    //***************************************************************************************************************//
    //                                        5: callback impl(2)                                                    //
    //***************************************************************************************************************//
    private static final class AnyCallback implements BeeTaskCallback {
        private final Thread callThread;
        private volatile BeeTaskHandle handle;

        AnyCallback() {
            this.callThread = Thread.currentThread();
        }

        public void onBefore(BeeTaskHandle handle) {
        }

        public void onReturn(Object result, BeeTaskHandle handle) {
            if (this.handle == null) {
                this.handle = handle;
                LockSupport.unpark(callThread);
            }
        }

        public void onCatch(Throwable e, BeeTaskHandle handle) {
        }

        public void onFinally(BeeTaskHandle handle) {
        }
    }

    private static final class AllCallback implements BeeTaskCallback {
        private final int taskSize;
        private final Thread callThread;
        private final AtomicInteger completedSize;

        AllCallback(int taskSize) {
            this.taskSize = taskSize;
            this.callThread = Thread.currentThread();
            this.completedSize = new AtomicInteger(0);
        }

        public void onBefore(BeeTaskHandle handle) {
        }

        public void onReturn(Object result, BeeTaskHandle handle) {
        }

        public void onCatch(Throwable e, BeeTaskHandle handle) {
        }

        public void onFinally(BeeTaskHandle handle) {
            if (completedSize.incrementAndGet() == taskSize) {
                LockSupport.unpark(callThread);
            }
        }
    }
}
