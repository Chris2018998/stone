/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beeop.*;
import org.stone.beeop.pool.exception.*;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.NCPU;

/**
 * keyed Keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class KeyedObjectPool implements BeeKeyedObjectPool {
    private static final Logger Log = LoggerFactory.getLogger(KeyedObjectPool.class);
    private static final AtomicIntegerFieldUpdater<KeyedObjectPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(KeyedObjectPool.class, "poolState");
    private final ConcurrentHashMap<Object, ObjectInstancePool> instancePoolMap = new ConcurrentHashMap<>(1);

    private String poolName;
    private int maxObjectKeySize;
    private volatile int poolState;
    private ObjectPoolHook exitHook;
    private long delayTimeForNextClearNs;//nanoseconds
    private boolean forceCloseUsingOnClear;

    private Object defaultKey;
    private ObjectInstancePool defaultPool;
    private ObjectPoolMonitorVo poolMonitorVo;
    private ThreadPoolExecutor servantService;
    private ScheduledThreadPoolExecutor scheduledService;

    //***************************************************************************************************************//
    //                1: pool initialize method(2)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeObjectSourceConfig config) throws Exception {
        //step1: config check
        if (config == null) throw new PoolInitializedException("Keyed object pool configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {
            try {
                startup(config.check());
                this.poolState = POOL_READY;
            } catch (Throwable e) {
                this.poolState = POOL_NEW;
                throw e;
            }
        } else {
            throw new PoolInitializedException("Keyed object pool has been initialized or in starting");
        }
    }

    private void startup(BeeObjectSourceConfig config) throws Exception {
        //step1:create default pool
        this.defaultPool = new ObjectInstancePool(config, this);
        this.poolName = config.getPoolName();
        RawObjectFactory objectFactory = config.getObjectFactory();
        this.defaultKey = objectFactory.getDefaultKey();
        this.defaultPool.startup(poolName, defaultKey,
                config.getInitialSize(), config.isAsyncCreateInitObject());
        this.instancePoolMap.put(defaultKey, defaultPool);

        //step2: copy some configured items to pool local variables
        this.maxObjectKeySize = config.getMaxObjectKeySize();
        this.forceCloseUsingOnClear = config.isForceCloseUsingOnClear();
        this.delayTimeForNextClearNs = MILLISECONDS.toNanos(config.getDelayTimeForNextClear());

        //step3: create servant executor
        int coreThreadSize = Math.min(NCPU, maxObjectKeySize);
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        if (this.servantService == null || servantService.getCorePoolSize() != coreThreadSize) {
            if (servantService != null) servantService.shutdown();
            this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 15,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(maxObjectKeySize), poolThreadFactory);
        }

        //step4: create idle-scan scheduled executor
        if (this.scheduledService == null) {
            scheduledService = new ScheduledThreadPoolExecutor(1, poolThreadFactory);
            scheduledService.scheduleWithFixedDelay(new IdleClearTask(this), 0,
                    config.getTimerCheckInterval(), MILLISECONDS);
        }

        //step5: register JVM hook
        if (this.exitHook == null) {
            this.exitHook = new ObjectPoolHook(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);
        }

        //step6: create a monitor object
        this.poolMonitorVo = new ObjectPoolMonitorVo(
                poolName,
                defaultPool.getPoolHostIP(),
                defaultPool.getPoolThreadId(),
                defaultPool.getPoolThreadName(),
                defaultPool.getPoolMode(),
                maxObjectKeySize * config.getMaxActive());
    }

    //***************************************************************************************************************//
    //                2: object borrow methods(2)                                                                    //                                                                                  //
    //***************************************************************************************************************//
    public final BeeObjectHandle getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Access forbidden,Keyed object pool was closed or in clearing");

        return defaultPool.getObjectHandle();
    }

    public final BeeObjectHandle getObjectHandle(Object key) throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Access forbidden,Keyed object pool was closed or in clearing");

        //1: get pool from generic map
        if (isDefaultKey(key)) return defaultPool.getObjectHandle();

        if (key == null) throw new ObjectKeyException("Object key can't be null");
        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null) return pool.getObjectHandle();

        //2: create pool by clone
        synchronized (instancePoolMap) {
            pool = instancePoolMap.get(key);
            if (pool == null) {
                if (instancePoolMap.size() >= maxObjectKeySize)
                    throw new ObjectGetException("Object key count reached max:" + maxObjectKeySize);

                pool = defaultPool.createByClone();
                pool.startup(poolName, key, 0, true);
                instancePoolMap.put(key, pool);
            }
        }

        //3: get handle from pool
        return pool.getObjectHandle();
    }

    //***************************************************************************************************************//
    //                3: key methods(7)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    public Object[] keys() {
        return this.instancePoolMap.keySet().toArray();
    }

    public Object getDefaultKey() {
        return this.defaultKey;
    }

    private boolean isDefaultKey(Object key) {
        return defaultKey == key || defaultKey.equals(key);
    }

    public void deleteKey(Object key) throws Exception {
        deleteKey(key, false);
    }

    public void deleteKey(Object key, boolean forceCloseUsing) throws Exception {
        if (key == null) throw new ObjectKeyException("Access forbidden,keyed object pool was closed or in clearing");
        if (isDefaultKey(key)) throw new ObjectKeyException("Default key forbid deletion");

        ObjectInstancePool pool = instancePoolMap.remove(key);
        if (pool != null && !pool.clear(forceCloseUsing))
            throw new PoolInClearingException("Keyed object sub pool was closed or in clearing");
    }

    public void deleteObjects(Object key, boolean forceCloseUsing) throws Exception {
        if (key == null) throw new ObjectKeyException("Access forbidden,keyed object pool was closed or in clearing");

        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null && !pool.clear(forceCloseUsing))
            throw new PoolInClearingException("Keyed object sub pool was closed or in clearing");
    }

    /**
     * get blocked time in connection creation a thread
     */
    public long getElapsedTimeSinceCreationLock(Object key) {
        ObjectInstancePool pool = instancePoolMap.get(key);
        return pool != null ? pool.getElapsedTimeSinceCreationLock() : 0L;
    }

    /**
     * interrupt queued waiters on creation lock and acquired thread,which may be stuck in driver
     */
    public void interruptThreadsOnCreationLock(Object key) {
        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null) pool.interruptThreadsOnCreationLock();
    }

    public BeeObjectPoolMonitorVo getKeyMonitorVo(Object key) throws Exception {
        if (key == null) throw new ObjectKeyException("Object key can't be null");

        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null) pool.getPoolMonitorVo();
        throw new ObjectKeyNotExistsException("Not found object key:" + key);
    }

    public void setPrintRuntimeLog(Object key, boolean indicator) throws Exception {
        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null) pool.setPrintRuntimeLog(indicator);
    }

    //***************************************************************************************************************//
    //                4: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //check pool is whether closed
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //close pool
    public void close() {
        do {
            int poolStateCode = this.poolState;
            if ((poolStateCode == POOL_NEW || poolStateCode == POOL_READY) && PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSED)) {
                for (ObjectInstancePool pool : instancePoolMap.values())
                    pool.close(forceCloseUsingOnClear);

                servantService.shutdown();
                scheduledService.shutdown();

                try {
                    Runtime.getRuntime().removeShutdownHook(this.exitHook);
                } catch (Throwable e) {
                    //do nothing
                }
                Log.info("BeeOP({})has shutdown", this.poolName);
                break;
            } else if (poolStateCode == POOL_CLOSED) {
                break;
            } else {
                LockSupport.parkNanos(delayTimeForNextClearNs);//block thread for next cas
            }
        } while (true);
    }

    //get pool monitor vo
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int semaphoreWaitingSize = 0;
        int transferWaitingSize = 0;
        int idleSize = 0, usingSize = 0;
        for (ObjectInstancePool pool : instancePoolMap.values()) {
            BeeObjectPoolMonitorVo monitorVo = pool.getPoolMonitorVo();
            idleSize = +monitorVo.getIdleSize();
            usingSize = +monitorVo.getUsingSize();
            semaphoreWaitingSize = +monitorVo.getSemaphoreWaitingSize();
            transferWaitingSize = +monitorVo.getTransferWaitingSize();
        }
        poolMonitorVo.setIdleSize(idleSize);
        poolMonitorVo.setUsingSize(usingSize);
        poolMonitorVo.setSemaphoreWaitingSize(semaphoreWaitingSize);
        poolMonitorVo.setTransferWaitingSize(transferWaitingSize);
        poolMonitorVo.setPoolState(poolState);
        return poolMonitorVo;
    }

    //enable Runtime Log
    public void setPrintRuntimeLog(boolean indicator) {
        for (ObjectInstancePool pool : instancePoolMap.values()) {
            pool.setPrintRuntimeLog(indicator);
        }
    }

    //remove all objects from pool
    public void clear(boolean forceCloseUsing) throws Exception {
        clear(forceCloseUsing, null);
    }

    //remove all objects from pool
    public void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {
        BeeObjectSourceConfig tempConfig = null;
        if (config != null) tempConfig = config.check();
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            for (ObjectInstancePool pool : instancePoolMap.values())
                pool.clear(forceCloseUsing);

            instancePoolMap.clear();//only place for clearing

            try {
                if (tempConfig != null) this.startup(tempConfig);
            } finally {
                this.poolState = POOL_READY;// reset state to POOL_READY
            }
        } else {
            throw new PoolInClearingException("Keyed object pool was closed or in clearing");
        }
    }

    //***************************************************************************************************************//
    //                5: idle close and async servant methods(2)                                                           //                                                                                  //
    //***************************************************************************************************************//
    void submitServantTask(Runnable task) {
        this.servantService.submit(task);
    }

    private void closeIdleTimeout() {
        for (ObjectInstancePool instancePool : instancePoolMap.values()) {
            instancePool.closeIdleTimeout();
        }
    }

    //***************************************************************************************************************//
    //                      6: Pool inner interface/class(3)                                                         //                                                                                  //
    //***************************************************************************************************************//
    private static class IdleClearTask implements Runnable {
        private final KeyedObjectPool pool;

        IdleClearTask(KeyedObjectPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                this.pool.closeIdleTimeout();
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    private static final class PoolThreadFactory implements ThreadFactory {
        private final String threadName;

        PoolThreadFactory(String threadName) {
            this.threadName = threadName;
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        }
    }

    private static class ObjectPoolHook extends Thread {
        private final KeyedObjectPool pool;

        ObjectPoolHook(KeyedObjectPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                Log.info("BeeOP({})JVM exit hook running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeOP({})Error occurred at closing key pool,cause:", this.pool.poolName, e);
            }
        }
    }
}
