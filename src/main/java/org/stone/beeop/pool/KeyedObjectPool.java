/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPool;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.pool.exception.ObjectGetForbiddenException;
import org.stone.beeop.pool.exception.ObjectKeyNotExistsException;
import org.stone.beeop.pool.exception.PoolInClearingException;
import org.stone.beeop.pool.exception.PoolInitializedException;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class KeyedObjectPool implements BeeObjectPool {
    private static final Object DEFAULT_KEY = new Object();
    private static final Logger Log = LoggerFactory.getLogger(KeyedObjectPool.class);
    private static final AtomicIntegerFieldUpdater<KeyedObjectPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(KeyedObjectPool.class, "poolState");
    private final Map<Object, ObjectGenericPool> genericPoolMap = new ConcurrentHashMap<>(1);

    private String poolName;
    private volatile int poolState;
    private ObjectPoolHook exitHook;
    private BeeObjectSourceConfig poolConfig;
    private ObjectGenericPool cloneGenericPool;//other generic pools clone base on it
    private ObjectGenericPool defaultGenericPool;
    private ObjectPoolMonitorVo poolMonitorVo;
    private ThreadPoolExecutor servantService;
    private ScheduledThreadPoolExecutor scheduledService;

    //***************************************************************************************************************//
    //                1: pool initialize method(2)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeObjectSourceConfig config) throws Exception {
        //step1: config check
        if (config == null) throw new PoolInitializedException("Configuration can't be null");
        BeeObjectSourceConfig checkedConfig = config.check();

        //step2: cas pool state to starting
        if (!PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING))
            throw new PoolInitializedException("Key pool has been initialized or in starting");

        //step3: pool startup
        try {
            this.poolConfig = checkedConfig;
            startup(poolConfig);
            this.poolState = POOL_READY;
        } catch (Exception e) {
            this.poolState = POOL_NEW;
            throw e;
        }
    }

    private void startup(BeeObjectSourceConfig config) throws Exception {
        //step1: create clone pool
        this.poolName = poolConfig.getPoolName();
        this.cloneGenericPool = new ObjectGenericPool(poolConfig, this);
        this.poolMonitorVo = new ObjectPoolMonitorVo(
                cloneGenericPool.getPoolHostIP(),
                cloneGenericPool.getPoolThreadId(),
                cloneGenericPool.getPoolThreadName(), poolName,
                cloneGenericPool.getPoolMode(), poolConfig.getMaxActive());

        //step2: register pool exit hook
        if (this.exitHook == null) {
            this.exitHook = new ObjectPoolHook(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);
        }

        //step5: calculate pool thread size and prepare thread factory
        int maxObjectKeySize = config.getMaxObjectKeySize();
        int cpuCoreSize = Runtime.getRuntime().availableProcessors();
        int coreThreadSize = Math.min(cpuCoreSize, maxObjectKeySize);
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);

        //step3: create servant executor(core thread keep alive)
        if (this.servantService == null || servantService.getCorePoolSize() != coreThreadSize) {
            if (servantService != null) servantService.shutdown();
            this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 15,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(maxObjectKeySize), poolThreadFactory);
        }

        //step7: create idle scheduled executor(core thread keep alive)
        if (this.scheduledService == null) {
            scheduledService = new ScheduledThreadPoolExecutor(1, poolThreadFactory);
            scheduledService.scheduleWithFixedDelay(new IdleClearTask(this), 0,
                    config.getTimerCheckInterval(), TimeUnit.MILLISECONDS);
        }

        //step4: create generic pool by init size
        if (config.getInitialSize() > 0) {
            ObjectGenericPool genericPool = cloneGenericPool.createByClone(config.getInitialObjectKey(),
                    poolName, config.getInitialSize(), config.isAsyncCreateInitObject());

            Object key = config.getInitialObjectKey();
            if (key == null) {
                key = DEFAULT_KEY;
                defaultGenericPool = genericPool;
            }
            genericPoolMap.put(key, genericPool);
        }
    }

    //***************************************************************************************************************//
    //                2: object borrow methods(2)                                                                          //                                                                                  //
    //***************************************************************************************************************//
    public final BeeObjectHandle getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Access forbidden,key pool was closed or in clearing");
        if (defaultGenericPool != null) return defaultGenericPool.getObjectHandle();
        return getObjectHandle(null);
    }

    public final BeeObjectHandle getObjectHandle(Object key) throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Access forbidden,key pool was closed or in clearing");

        //1: get pool from generic map
        if (key == null) key = DEFAULT_KEY;
        ObjectGenericPool pool = genericPoolMap.get(key);
        if (pool != null) return pool.getObjectHandle();

        //2: create pool by clone
        synchronized (genericPoolMap) {
            pool = genericPoolMap.get(key);
            if (pool == null) {
                pool = cloneGenericPool.createByClone(key == DEFAULT_KEY ? null : key, poolName, 0, true);
                genericPoolMap.put(key, pool);
                if (key == DEFAULT_KEY) defaultGenericPool = pool;
            }
        }

        //3: get handle from pool
        return pool.getObjectHandle();
    }

    //***************************************************************************************************************//
    //                3: key methods(7)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    public Object[] keys() {
        Object[] keys = this.genericPoolMap.keySet().toArray();
        for (int i = 0, l = keys.length; i < l; i++) {
            if (keys[i] == DEFAULT_KEY) {
                keys[i] = null;
                break;
            }
        }
        return keys;
    }

    public void clear(Object key) throws Exception {
        clear(key, false);
    }

    public void clear(Object key, boolean forceCloseUsing) throws Exception {
        ObjectGenericPool pool = genericPoolMap.get(key);
        if (pool != null) {
            if (!pool.clear(forceCloseUsing))
                throw new PoolInClearingException("Generic object pool was closed or in clearing");
        } else {
            throw new ObjectKeyNotExistsException("Not found object generic pool with key:" + key);
        }
    }

    public void deleteKey(Object key) throws Exception {
        deleteKey(key, false);
    }

    public void deleteKey(Object key, boolean forceCloseUsing) throws Exception {
        if (key == null) key = DEFAULT_KEY;
        ObjectGenericPool pool = genericPoolMap.remove(key);
        if (pool != null) {
            if (!pool.clear(forceCloseUsing))
                throw new PoolInClearingException("Generic object pool was closed or in clearing");
            if (key == DEFAULT_KEY) defaultGenericPool = null;
        } else {
            throw new ObjectKeyNotExistsException("Not found object generic pool with key:" + key);
        }
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo(Object key) throws Exception {
        ObjectGenericPool pool = genericPoolMap.get(key);
        if (pool != null) pool.getPoolMonitorVo();
        throw new ObjectKeyNotExistsException("Not found object generic pool with key:" + key);
    }

    public void setPrintRuntimeLog(Object key, boolean indicator) throws Exception {
        ObjectGenericPool pool = genericPoolMap.get(key);
        if (pool != null) pool.setPrintRuntimeLog(indicator);
        throw new ObjectKeyNotExistsException("Not found object generic pool with key:" + key);
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
        long delayTimeForNextClearNs = TimeUnit.MILLISECONDS.toNanos(poolConfig.getDelayTimeForNextClear());
        do {
            int poolStateCode = this.poolState;
            if ((poolStateCode == POOL_NEW || poolStateCode == POOL_READY) && PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSED)) {
                for (ObjectGenericPool pool : genericPoolMap.values())
                    pool.close(poolConfig.isForceCloseUsingOnClear());

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
                LockSupport.parkNanos(delayTimeForNextClearNs);// default wait 3 seconds
            }
        } while (true);
    }

    //get pool monitor vo
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int semaphoreWaitingSize = 0;
        int transferWaitingSize = 0;
        int idleSize = 0, usingSize = 0, maxSize = 0;
        for (ObjectGenericPool pool : genericPoolMap.values()) {
            BeeObjectPoolMonitorVo genericMonitorVo = pool.getPoolMonitorVo();
            idleSize = +genericMonitorVo.getIdleSize();
            usingSize = +genericMonitorVo.getUsingSize();
            maxSize = +genericMonitorVo.getPoolMaxSize();
            semaphoreWaitingSize = +genericMonitorVo.getSemaphoreWaitingSize();
            transferWaitingSize = +genericMonitorVo.getTransferWaitingSize();
        }
        poolMonitorVo.setMaxSize(maxSize);
        poolMonitorVo.setIdleSize(idleSize);
        poolMonitorVo.setUsingSize(usingSize);
        poolMonitorVo.setSemaphoreWaitingSize(semaphoreWaitingSize);
        poolMonitorVo.setTransferWaitingSize(transferWaitingSize);
        poolMonitorVo.setPoolState(poolState);
        return poolMonitorVo;
    }

    //enable Runtime Log
    public void setPrintRuntimeLog(boolean indicator) {
        for (ObjectGenericPool pool : genericPoolMap.values()) {
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
            for (ObjectGenericPool pool : genericPoolMap.values())
                pool.clear(forceCloseUsing);

            genericPoolMap.clear();//only place for clearing

            try {
                if (tempConfig != null) {
                    this.poolConfig = tempConfig;
                    this.startup(tempConfig);
                }
            } finally {
                this.poolState = POOL_READY;// restore state;
            }
        } else {
            throw new PoolInClearingException("Key pool was closed or in clearing");
        }
    }

    //***************************************************************************************************************//
    //                5: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//
    void submitServantTask(Runnable task) {
        this.servantService.submit(task);
    }

    private void closeIdleTimeout() {
        for (ObjectGenericPool pool : genericPoolMap.values()) {
            pool.closeIdleTimeout();
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
                Log.info("BeeOP({})Exit hook running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeOP({})Error occurred at closing pool,cause:", this.pool.poolName, e);
            }
        }
    }
}
