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
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.NCPU;
import static org.stone.tools.CommonUtil.getArrayIndex;

/**
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class KeyedObjectPool implements BeeKeyedObjectPool {
    static final Logger Log = LoggerFactory.getLogger(KeyedObjectPool.class);
    private static final AtomicIntegerFieldUpdater<KeyedObjectPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(KeyedObjectPool.class, "poolState");
    private final ConcurrentHashMap<Object, ObjectInstancePool> instancePoolMap = new ConcurrentHashMap<>(1);

    String poolName;
    private volatile int poolState;

    //key size of sub pools
    private int maxSubPoolSize;
    //a pooled key map to default sub pool
    private Object defaultKey;
    //default sub pool,which must exists
    private ObjectInstancePool defaultPool;
    //an array of locks for creating sub pools
    private ReentrantLock[] subPoolsCreationLocks;

    //wait time for borrowed objects return to pool,time unit is nanoseconds
    private long delayTimeForNextClearNs;
    //close borrowed objects immediately on cleaning pool
    private boolean forceCloseUsingOnClear;
    //a monitor object of this key pool
    private ObjectPoolMonitorVo poolMonitorVo;
    //a thread pool run tasks to search idles or create objects,then transfer to waiters
    private ThreadPoolExecutor servantService;
    //a timed value for interval execute to scheduled tasks
    private long timerCheckInterval;
    //a scheduled tasks pool works to scan timeout objects and clean them(idle timeout and hold timeout)
    private ScheduledThreadPoolExecutor scheduledService;

    private BeeObjectSourceConfig poolConfig;
    //A Hook thread to close pool when JVM exit
    private ObjectPoolHook exitHook;

    //***************************************************************************************************************//
    //                1: Methods to operation on keyed pool(9)                                                       //                                                                                  //
    //***************************************************************************************************************//
    //1.1: initializes pool with a parameter configuration
    public void init(BeeObjectSourceConfig config) throws Exception {
        //step1: config check
        if (config == null) throw new PoolInitializeFailedException("Object pool configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {
            try {
                this.poolConfig = config.check();
                startup(poolConfig);
                this.poolState = POOL_READY;
            } catch (Throwable e) {
                this.poolState = POOL_NEW;//reset to new state when fail
                throw e;
            }
        } else {
            throw new PoolInitializeFailedException("Object keyed pool has initialized or in starting");
        }
    }

    //1.2: An internal method to startup pool with a checked configuration
    private void startup(BeeObjectSourceConfig config) throws Exception {
        //step1: create default sub pool and startup it.
        this.poolName = config.getPoolName();
        BeeObjectFactory objectFactory = config.getObjectFactory();
        this.defaultKey = objectFactory.getDefaultKey();
        this.defaultPool = new ObjectInstancePool(config, this);
        this.defaultPool.startup(poolName, defaultKey,
                config.getInitialSize(), config.isAsyncCreateInitObject());
        this.instancePoolMap.put(defaultKey, defaultPool);

        //step2: create locks for sub pools creation
        this.maxSubPoolSize = config.getMaxObjectKeySize();
        this.subPoolsCreationLocks = new ReentrantLock[maxSubPoolSize];
        for (int i = 0; i < maxSubPoolSize; i++)
            subPoolsCreationLocks[i] = new ReentrantLock();
        this.forceCloseUsingOnClear = config.isForceCloseUsingOnClear();
        this.delayTimeForNextClearNs = MILLISECONDS.toNanos(config.getDelayTimeForNextClear());

        //step3: create a common thread pool to service all sub pools(search idle or create new and transfer to waiters)
        int coreThreadSize = Math.min(NCPU, maxSubPoolSize);
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        if (this.servantService == null)
            this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 15,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<>(maxSubPoolSize), poolThreadFactory);
        if (servantService.getCorePoolSize() != coreThreadSize)
            this.servantService.setCorePoolSize(coreThreadSize);

        //step4: create a common scheduled thread pool to service all sub pools(scan out idle timeout and hold timeout)
        if (this.scheduledService == null)
            this.scheduledService = new ScheduledThreadPoolExecutor(coreThreadSize, poolThreadFactory);
        if (scheduledService.getCorePoolSize() != coreThreadSize)
            this.scheduledService.setCorePoolSize(coreThreadSize);
        this.timerCheckInterval = config.getTimerCheckInterval();
        this.scheduledService.scheduleWithFixedDelay(new TimeoutScanTask(defaultPool), timerCheckInterval,
                timerCheckInterval, MILLISECONDS);

        //step5: create a JVM hook for keyed pool
        if (this.exitHook == null) {
            this.exitHook = new ObjectPoolHook(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);
        }

        //step6: create monitor object of keyed pool
        this.poolMonitorVo = new ObjectPoolMonitorVo(
                poolName,
                defaultPool.getPoolHostIP(),
                defaultPool.getPoolThreadId(),
                defaultPool.getPoolThreadName(),
                defaultPool.getPoolMode(),
                maxSubPoolSize * config.getMaxActive());
    }

    //1.3: query this keyed pool state is whether closed
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //1.4: closes this keyed pool
    public void close() {
        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_CLEARING) {
                LockSupport.parkNanos(this.delayTimeForNextClearNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                Log.info("BeeOP({})Begin to shutdown", this.poolName);
                for (ObjectInstancePool pool : instancePoolMap.values())
                    pool.close(forceCloseUsingOnClear);

                servantService.shutdown();
                scheduledService.shutdown();

                try {
                    Runtime.getRuntime().removeShutdownHook(this.exitHook);
                } catch (Throwable e) {
                    //do nothing
                }

                this.poolState = POOL_CLOSED;
                Log.info("BeeOP({})has shutdown", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    //1.5: enable runtime logs print or disable print by a boolean switch
    public void setPrintRuntimeLog(boolean indicator) {
        for (ObjectInstancePool pool : instancePoolMap.values()) {
            pool.setPrintRuntimeLog(indicator);
        }
    }

    //1.6: get monitor object of this keyed pool
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int semaphoreWaitingSize = 0;
        int transferWaitingSize = 0;
        int idleSize = 0, usingSize = 0;
        for (ObjectInstancePool pool : instancePoolMap.values()) {
            BeeObjectPoolMonitorVo monitorVo = pool.getPoolMonitorVo();
            idleSize += monitorVo.getIdleSize();
            usingSize += monitorVo.getUsingSize();
            semaphoreWaitingSize += monitorVo.getSemaphoreWaitingSize();
            transferWaitingSize += monitorVo.getTransferWaitingSize();
        }
        poolMonitorVo.setIdleSize(idleSize);
        poolMonitorVo.setUsingSize(usingSize);
        poolMonitorVo.setSemaphoreWaitingSize(semaphoreWaitingSize);
        poolMonitorVo.setTransferWaitingSize(transferWaitingSize);
        poolMonitorVo.setPoolState(poolState);
        return poolMonitorVo;
    }

    //1.7: remove all pooled objects
    public void clear(boolean forceCloseUsing) throws Exception {
        clear(forceCloseUsing, false, null);
    }

    //1.8: remove all pooled objects and reinitialize pool with a new configuration
    public void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {
        clear(forceCloseUsing, true, config);
    }

    //1.9: remove all pooled objects and if parameter reInit is true,then reinitialize pool with a new configuration
    private void clear(boolean forceCloseUsing, boolean reinit, BeeObjectSourceConfig config) throws Exception {
        if (reinit && config == null)
            throw new BeeObjectSourceConfigException("Configuration for pool reinitialization can' be null");

        //clean pool after cas pool state success
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            try {
                //check the parameter configuration,if fail then exit method since here
                BeeObjectSourceConfig checkedConfig = null;
                if (reinit) checkedConfig = config.check();

                //clean sub pools one by one
                Log.info("BeeOP({})begin to remove all connections", this.poolName);
                for (ObjectInstancePool pool : instancePoolMap.values())
                    pool.clear(forceCloseUsing);
                instancePoolMap.clear();
                Log.info("BeeOP({})completed to remove all connections", this.poolName);

                if (reinit) {
                    this.poolConfig = checkedConfig;
                    Log.info("BeeOP({})start to reinitialize keyed pool", this.poolName);
                    this.startup(checkedConfig);//throws Exception only fail to create initial objects for default pool or fail to set default values
                    //note: if failed,this method may be recalled with correct configuration
                    Log.info("BeeOP({})completed to reinitialize pool successful", this.poolName);
                }
            } finally {
                this.poolState = POOL_READY;//reset pool state to ready
            }
        } else {
            throw new PoolInClearingException("Object keyed pool was closed or in cleaning");
        }
    }

    //***************************************************************************************************************//
    //                                    2: Methods to borrow pooled objects(2)                                     //
    //***************************************************************************************************************//
    //2.1: gets an object from default sub pool
    public BeeObjectHandle getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Object keyed pool was closed or in cleaning");

        return defaultPool.getObjectHandle();
    }

    //2.2: gets an object from sub pool map to given key
    public BeeObjectHandle getObjectHandle(Object key) throws Exception {
        this.checkParameterKey(key);
        //1:if key is equal to default key,then get object from default sub pool
        if (isDefaultKey(key)) return defaultPool.getObjectHandle();

        //2: get sub pool from ConcurrentHashMap with given key
        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null) return pool.getObjectHandle();

        //3: create a sub pool under lock
        ReentrantLock lock = subPoolsCreationLocks[getArrayIndex(key.hashCode(), maxSubPoolSize)];
        try {
            if (lock.tryLock(defaultPool.getMaxWaitNs(), TimeUnit.NANOSECONDS)) {
                try {
                    pool = instancePoolMap.get(key);
                    if (pool == null) {
                        if (instancePoolMap.size() == maxSubPoolSize)
                            throw new ObjectGetException("The count of pooled keys has reached max size:" + maxSubPoolSize);

                        //create a new sub pool by clone and submit it to schedule pool
                        pool = defaultPool.createByClone();
                        pool.startup(poolName, key, 0, true);
                        instancePoolMap.put(key, pool);
                        this.scheduledService.scheduleWithFixedDelay(new TimeoutScanTask(pool), timerCheckInterval,
                                timerCheckInterval, MILLISECONDS);
                    }
                } finally {
                    lock.unlock();
                }

                //4: get handle from pool
                return pool.getObjectHandle();
            } else {
                throw new ObjectGetTimeoutException("Waited timeout to acquire lock to create sub pool,related key:" + key);
            }
        } catch (InterruptedException e) {
            throw new ObjectGetInterruptedException("An interruption occurred while waiting to acquire lock to create sub pool,related key:" + key);
        }
    }

    //***************************************************************************************************************//
    //                                    3: Methods to maintain pooed keys(5)                                       //
    //***************************************************************************************************************//
    public int getObjectCreatingCount(Object key) throws Exception {
        this.checkParameterKey(key);

        ObjectInstancePool pool = instancePoolMap.get(key);
        return pool != null ? pool.getObjectCreatingCount() : 0;
    }

    public int getObjectCreatingTimeoutCount(Object key) throws Exception {
        this.checkParameterKey(key);

        ObjectInstancePool pool = instancePoolMap.get(key);
        return pool != null ? pool.getObjectCreatingTimeoutCount() : 0;
    }

    public Thread[] interruptObjectCreating(Object key, boolean interruptTimeout) throws Exception {
        this.checkParameterKey(key);

        ObjectInstancePool pool = instancePoolMap.get(key);
        return pool != null ? pool.interruptObjectCreating(interruptTimeout) : null;
    }

    public void setPrintRuntimeLog(Object key, boolean indicator) throws Exception {
        this.checkParameterKey(key);

        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null) pool.setPrintRuntimeLog(indicator);
    }

    public BeeObjectPoolMonitorVo getMonitorVo(Object key) throws Exception {
        this.checkParameterKey(key);

        ObjectInstancePool pool = instancePoolMap.get(key);
        if (pool != null) pool.getPoolMonitorVo();
        throw new ObjectKeyNotExistsException("Not found object key:" + key);
    }

    //***************************************************************************************************************//
    //                                    4: Methods to maintain pooed keys(3)                                        //
    //***************************************************************************************************************//
    public Object[] keys() {
        return this.instancePoolMap.keySet().toArray();
    }

    public void deleteKey(Object key) throws Exception {
        deleteKey(key, false);
    }

    public void deleteKey(Object key, boolean forceCloseUsing) throws Exception {
        this.checkParameterKey(key);

        ObjectInstancePool pool = instancePoolMap.remove(key);
        if (pool != null && !pool.clear(forceCloseUsing))
            throw new PoolInClearingException("Keyed sub pool was closed or in cleaning");
    }

    //***************************************************************************************************************//
    //                5: private methods and friendly methods (3)                                                    //                                                                                  //
    //***************************************************************************************************************//
    void submitServantTask(Runnable task) {
        this.servantService.submit(task);
    }

    private boolean isDefaultKey(Object key) {
        return defaultKey == key || defaultKey.equals(key);
    }

    private void checkParameterKey(Object key) throws Exception {
        if (key == null) throw new ObjectKeyException("Object key can't be null");
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Object keyed pool was closed or in cleaning");
    }

    //***************************************************************************************************************//
    //                      6:  internal classes(3)                                                                  //                                                                                  //
    //***************************************************************************************************************//
    private static class TimeoutScanTask implements Runnable {
        private final ObjectInstancePool pool;

        TimeoutScanTask(ObjectInstancePool pool) {
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

        @Override
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
