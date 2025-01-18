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
    //key size of object varieties
    private int maxSubPoolSize;
    //Key is for default object variety
    private Object defaultKey;
    //Instance pool of default object variety
    private ObjectInstancePool defaultPool;
    //An array of locks for object creation
    private ReentrantLock[] subPoolsCreationLocks;

    //A boolean control argument for borrowed object on pool clean,true is that force recycle them immediately,otherwise that wait them return to pool,then physical close them,default is false.
    private boolean forceCloseUsingOnClear;
    //Monitor object of pool
    private ObjectPoolMonitorVo poolMonitorVo;
    //A thread pool run servant tasks to search idle objects or create new objects for waiters
    private ThreadPoolExecutor servantService;
    //An interval time for scheduled executor to run idle-scan tasks
    private long timerCheckInterval;
    //A scheduled executor to scan timeout objects(idle timeout and hold timeout)
    private ScheduledThreadPoolExecutor scheduledService;

    private BeeObjectSourceConfig poolConfig;
    //A Hook to close pool when JVM shutdown
    private ObjectPoolHook exitHook;

    //***************************************************************************************************************//
    //                              1: Pool initializes                                                              //                                                                                  //
    //***************************************************************************************************************//
    //1.1: Pool initializes.
    public void init(BeeObjectSourceConfig config) throws Exception {
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
            throw new PoolInitializeFailedException("Object keyed pool has initialized or in initializing");
        }
    }

    //1.2: Launch pool with check passed configuration
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
        this.forceCloseUsingOnClear = config.isForceRecycleBorrowedOnClose();
        this.maxSubPoolSize = config.getMaxObjectKeySize();
        this.subPoolsCreationLocks = new ReentrantLock[maxSubPoolSize];
        for (int i = 0; i < maxSubPoolSize; i++)
            subPoolsCreationLocks[i] = new ReentrantLock();

        //step3: create thread pool and schedule pool
        if (this.servantService != null) servantService.shutdownNow();
        if (this.scheduledService != null) scheduledService.shutdownNow();
        int coreThreadSize = Math.min(NCPU, maxSubPoolSize);
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 15L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(maxSubPoolSize), poolThreadFactory);
        this.scheduledService = new ScheduledThreadPoolExecutor(coreThreadSize, poolThreadFactory);
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

    //***************************************************************************************************************//
    //                              2: Pool Close(2)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    //2.1: query this keyed pool state is whether closed
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //2.2: closes this keyed pool
    public void close() {
        final long parkTimeForRetryNs = defaultPool.getParkTimeForRetryNs();

        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_CLEARING) {
                LockSupport.parkNanos(parkTimeForRetryNs);//delay and retry
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

    //***************************************************************************************************************//
    //                              3: Pool Clean(3)                                                                  //                                                                                  //
    //***************************************************************************************************************//
    //3.1: remove all pooled objects
    public void clear(boolean forceCloseUsing) throws Exception {
        clear(forceCloseUsing, false, null);
    }

    //3.2: remove all pooled objects and reinitialize pool with a new configuration
    public void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {
        clear(forceCloseUsing, true, config);
    }

    //3.3: remove all pooled objects and if parameter reInit is true,then reinitialize pool with a new configuration
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
                Log.info("BeeOP({})completed to remove all connections", this.poolName);

                if (reinit) {
                    this.defaultKey = null;
                    this.defaultPool = null;
                    instancePoolMap.clear();
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
    //                              4: Pool Monitoring and log print(2)                                              //                                                                                  //
    //***************************************************************************************************************//
    //4.1: enable runtime logs print or disable print by a boolean switch
    public void setPrintRuntimeLog(boolean indicator) {
        for (ObjectInstancePool pool : instancePoolMap.values()) {
            pool.setPrintRuntimeLog(indicator);
        }
    }

    //4.2: get monitor object of this keyed pool
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int semaphoreWaitingSize = 0;
        int transferWaitingSize = 0;
        int idleSize = 0, usingSize = 0;
        for (ObjectInstancePool pool : instancePoolMap.values()) {
            BeeObjectPoolMonitorVo monitorVo = pool.getPoolMonitorVo();
            idleSize += monitorVo.getIdleSize();
            usingSize += monitorVo.getBorrowedSize();
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

    //***************************************************************************************************************//
    //                                    5: Object getting(2)                                                       //
    //***************************************************************************************************************//
    //2.1: gets an object from default sub pool
    public BeeObjectHandle getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Object keyed pool was closed or in cleaning");

        return defaultPool.getObjectHandle();
    }

    //2.2: gets an object from sub pool map to given key
    public BeeObjectHandle getObjectHandle(Object key) throws Exception {
        //1: check inputted key
        this.checkParameterKey(key);

        //2: if key is equal to default key then attempt to get object a handle from default pool
        if (isDefaultKey(key)) return defaultPool.getObjectHandle();

        //3: get category pool from instancePoolMap by key,if not reach max capacity
        ObjectInstancePool categoryPool = instancePoolMap.get(key);
        if (categoryPool != null) return categoryPool.getObjectHandle();

        //4: if category capacity reach max,then throws an exception with fulled message
        if (instancePoolMap.size() == this.maxSubPoolSize)
            throw new ObjectKeyException("The capacity of object category has reach max size:" + maxSubPoolSize);

        //5: create a new category pool with key under a lock
        ReentrantLock lock = subPoolsCreationLocks[getArrayIndex(key.hashCode(), maxSubPoolSize)];
        try {
            if (lock.tryLock(defaultPool.getMaxWaitNs(), TimeUnit.NANOSECONDS)) {
                try {
                    categoryPool = instancePoolMap.get(key);
                    if (categoryPool == null) {
                        if (instancePoolMap.size() == maxSubPoolSize)
                            throw new ObjectGetException("The capacity of object category has reach max size:" + maxSubPoolSize);

                        //clone a category pool
                        categoryPool = defaultPool.createByClone();
                        categoryPool.startup(poolName, key, 0, true);
                        instancePoolMap.put(key, categoryPool);
                        this.scheduledService.scheduleWithFixedDelay(new TimeoutScanTask(categoryPool), timerCheckInterval,
                                timerCheckInterval, MILLISECONDS);
                    }
                } finally {
                    lock.unlock();
                }

                //6: get handle from pool
                return categoryPool.getObjectHandle();
            } else {
                throw new ObjectGetTimeoutException("Waited timeout to acquire lock to create sub pool,related key:" + key);
            }
        } catch (InterruptedException e) {
            throw new ObjectGetInterruptedException("An interruption occurred while waiting to acquire lock to create sub pool,related key:" + key);
        }
    }

    //***************************************************************************************************************//
    //                                    6:Operation with Variety Key(10)                                           //
    //***************************************************************************************************************//
    public Object[] keys() {
        return this.instancePoolMap.keySet().toArray();
    }

    public boolean exists(Object key) {
        return instancePoolMap.containsKey(key);
    }

    public void clear(Object key) throws Exception {
        clear(key, false);
    }

    public void clear(Object key, boolean forceRecycleBorrowed) throws Exception {
        if (!getObjectInstancePool(key).clear(forceRecycleBorrowed))
            throw new PoolInClearingException("Keyed sub pool was closed or in cleaning");
    }

    public void deleteKey(Object key) throws Exception {
        deleteKey(key, false);
    }

    public void deleteKey(Object key, boolean forceRecycleBorrowed) throws Exception {
        if (!removeObjectInstancePool(key).clear(forceRecycleBorrowed))
            throw new PoolInClearingException("Keyed sub pool was closed or in cleaning");
    }

    public boolean isPrintRuntimeLog(Object key) throws Exception {
        return getObjectInstancePool(key).isPrintRuntimeLog();
    }

    public void setPrintRuntimeLog(Object key, boolean indicator) throws Exception {
        getObjectInstancePool(key).setPrintRuntimeLog(indicator);
    }

    public BeeObjectPoolMonitorVo getMonitorVo(Object key) throws Exception {
        return getObjectInstancePool(key).getPoolMonitorVo();
    }

    public Thread[] interruptObjectCreating(Object key, boolean interruptTimeout) throws Exception {
        return getObjectInstancePool(key).interruptObjectCreating(interruptTimeout);
    }

    //***************************************************************************************************************//
    //                7: Private methods and friendly methods (4)                                                    //                                                                                  //
    //***************************************************************************************************************//
    void submitServantTask(Runnable task) {
        this.servantService.submit(task);
    }

    private boolean isDefaultKey(Object key) {
        return defaultKey == key || defaultKey.equals(key);
    }

    private void checkParameterKey(Object key) throws Exception {
        if (key == null) throw new ObjectKeyException("Object category key can't be null or empty");
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Object keyed pool was not ready");
    }

    private ObjectInstancePool removeObjectInstancePool(Object key) throws Exception {
        checkParameterKey(key);

        if (isDefaultKey(key)) throw new ObjectKeyException("Default key is forbidden to delete");
        ObjectInstancePool categoryPool = instancePoolMap.remove(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Object category key not exists or has been removed");
        return categoryPool;
    }

    private ObjectInstancePool getObjectInstancePool(Object key) throws Exception {
        checkParameterKey(key);

        if (isDefaultKey(key)) return defaultPool;
        ObjectInstancePool categoryPool = instancePoolMap.get(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Object category key not exists or has been removed");
        return categoryPool;
    }

    //***************************************************************************************************************//
    //                      9: Internal classes(3)                                                                  //                                                                                  //
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
