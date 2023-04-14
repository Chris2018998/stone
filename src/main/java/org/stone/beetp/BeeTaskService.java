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
    public <T> T invokeAny(Collection<? extends BeeTask<T>> tasks) throws Exception {
        return invokeAny(tasks, 0, TimeUnit.NANOSECONDS);
    }

    public <T> T invokeAny(Collection<? extends BeeTask<T>> tasks, long timeout, TimeUnit unit) throws Exception {
        AnyCallback callback = new AnyCallback();
        //@todo
        return null;
    }

    public <T> List<BeeTaskHandle<T>> invokeAll(Collection<? extends BeeTask<T>> tasks) throws InterruptedException {
        return invokeAll(tasks, 0, TimeUnit.NANOSECONDS);
    }

    public <T> List<BeeTaskHandle<T>> invokeAll(Collection<? extends BeeTask<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        AllCallback callback = new AllCallback(tasks.size());
        //@todo
        return null;
    }

    //***************************************************************************************************************//
    //                                        5: callback impl(2)                                                   //
    //***************************************************************************************************************//
    private static final class AnyCallback implements BeeTaskCallback {
        private final Thread callThread;
        private volatile BeeTaskHandle handle;

        AnyCallback() {
            this.callThread = Thread.currentThread();
        }

        public void beforeCall(BeeTaskHandle handle) {
        }

        public void afterThrowing(Throwable e, BeeTaskHandle handle) {
        }

        public void atFinally(BeeTaskHandle handle) {
        }

        public void afterCall(Object result, BeeTaskHandle handle) {
            if (this.handle == null) {
                this.handle = handle;
                LockSupport.unpark(callThread);
            }
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

        public void beforeCall(BeeTaskHandle handle) {
        }

        public void afterCall(Object result, BeeTaskHandle handle) {
        }

        public void afterThrowing(Throwable e, BeeTaskHandle handle) {
        }

        public void atFinally(BeeTaskHandle handle) {
            if (completedSize.incrementAndGet() == taskSize) {
                LockSupport.unpark(callThread);
            }
        }
    }
}
