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

import org.stone.beetp.pool.exception.TaskExecutedException;
import org.stone.beetp.pool.exception.TaskRelatedTimeoutException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    //                                        2: task submit methods(4)                                              //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.submit(task);
        return createPoolByLock().submit(task);
    }

    public BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.submit(task, callback);
        return createPoolByLock().submit(task, callback);
    }

    public BeeTaskHandle submit(BeeTaskConfig taskConfig) throws BeeTaskException, BeeTaskPoolException {
        if (this.ready) return pool.submit(taskConfig);
        return createPoolByLock().submit(taskConfig);
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
    //                                        3: pool terminate methods(4)                                           //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return pool == null || pool.isTerminated();
    }

    public boolean isTerminating() {
        return pool == null || pool.isTerminating();
    }

    public BeeTaskPoolMonitorVo getPoolMonitorVo() throws BeeTaskPoolException {
        if (pool == null) throw new BeeTaskPoolException("Task pool not be initialized");
        return pool.getPoolMonitorVo();
    }

    public void terminate(boolean cancelRunningTask) throws BeeTaskPoolException {
        if (pool != null) pool.terminate(cancelRunningTask);
    }

    //***************************************************************************************************************//
    //                                        4: tasks invoke(4)                                                     //
    //***************************************************************************************************************//
    public BeeTaskHandle invokeAny(Collection<? extends BeeTask> tasks) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        return invokeAny(tasks, 0, TimeUnit.NANOSECONDS);
    }

    public BeeTaskHandle invokeAny(Collection<? extends BeeTask> tasks, long timeout, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        if (tasks == null || tasks.isEmpty()) throw new IllegalArgumentException("Task list can't be null or empty");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        if (timeout < 0) throw new IllegalArgumentException("Time can't be less than zero");

        //1: check pool is whether ready
        if (!this.ready) pool = createPoolByLock();

        //2: prepare callback before submitting to pool
        BeeTaskHandle completedHandle = null;
        AnyCallback callback = new AnyCallback();
        List<BeeTaskHandle> handleList = new ArrayList<>(tasks.size());
        boolean timed = timeout > 0;
        long deadline = timed ? System.nanoTime() + unit.toNanos(timeout) : 0;

        try {
            //3: submit tasks to pool and try to read
            for (BeeTask task : tasks) {
                completedHandle = callback.handle;
                if (completedHandle != null) break;
                handleList.add(pool.submit(task, callback));
            }

            //4:try to read a completed handle from callback
            if (completedHandle == null) {
                do {
                    completedHandle = callback.handle;
                    if (completedHandle != null) break;

                    if (timed) {
                        long parkTime = deadline - System.nanoTime();
                        if (parkTime <= 0) throw new TaskRelatedTimeoutException("Timeout");
                        LockSupport.parkNanos(parkTime);
                    } else {
                        LockSupport.park();
                    }

                    if (Thread.interrupted()) throw new InterruptedException();
                } while (true);
            }

            return completedHandle;//reach here,the handle must be set
        } catch (InterruptedException e) {
            throw e;
        } catch (BeeTaskException e) {
            throw e;
        } catch (Throwable e) {
            throw new TaskExecutedException(e.getCause());
        } finally {
            for (BeeTaskHandle handle : handleList)
                if (!handle.isDone()) handle.cancel(true);
        }
    }

    public List<BeeTaskHandle> invokeAll(Collection<? extends BeeTask> tasks) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        return invokeAll(tasks, 0, TimeUnit.NANOSECONDS);
    }

    //return done handle
    public List<BeeTaskHandle> invokeAll(Collection<? extends BeeTask> tasks, long timeout, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException, InterruptedException {
        if (tasks == null || tasks.isEmpty()) throw new IllegalArgumentException("Task list can't be null or empty");
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        if (timeout < 0) throw new IllegalArgumentException("Time can't be less than zero");

        //1: check pool is whether ready
        if (!this.ready) pool = createPoolByLock();

        //2: prepare callback before submitting to pool
        boolean done = false;
        int totalSize = tasks.size();
        AllCallback callback = new AllCallback(totalSize);
        List<BeeTaskHandle> handleList = new ArrayList<>(totalSize);//submitted list
        boolean timed = timeout > 0;
        long deadline = timed ? System.nanoTime() + unit.toNanos(timeout) : 0;

        try {
            //3: submit all tasks to pool
            for (BeeTask task : tasks) handleList.add(pool.submit(task, callback));

            //4: if all done,then break;
            do {
                if (callback.doneSize.get() == totalSize) break;

                if (timed) {
                    long parkTime = deadline - System.nanoTime();
                    if (parkTime <= 0) break;//timeout,then break
                    LockSupport.parkNanos(parkTime);
                } else {
                    LockSupport.park();
                }
                
                if (Thread.interrupted()) throw new InterruptedException();
            } while (true);

            return new ArrayList<>(callback.handleQueueInDone);
        } finally {
            for (BeeTaskHandle handle : handleList)
                if (!handle.isDone()) handle.cancel(true);
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
        private final Thread callThread;
        private final int taskTotalSize;
        private final AtomicInteger doneSize;
        private final Queue<BeeTaskHandle> handleQueueInDone;

        AllCallback(int taskSize) {
            this.taskTotalSize = taskSize;
            this.callThread = Thread.currentThread();
            this.doneSize = new AtomicInteger(0);
            this.handleQueueInDone = new ConcurrentLinkedQueue<>();
        }

        public void onBefore(BeeTaskHandle handle) {
        }

        public void onReturn(Object result, BeeTaskHandle handle) {
        }

        public void onCatch(Throwable e, BeeTaskHandle handle) {
        }

        public void onFinally(BeeTaskHandle handle) {
            handleQueueInDone.offer(handle);
            if (this.doneSize.incrementAndGet() == taskTotalSize) {//all tasks done,so wakeup caller thread
                LockSupport.unpark(callThread);
            }
        }
    }
}
