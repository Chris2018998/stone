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
    private final ConcurrentHashMap<Object, ObjectInstancePool> categoryPoolMap = new ConcurrentHashMap<>(1);
    String poolName;

    private volatile int poolState;
    //max size of object category
    private int categoryMaxSize;
    //Key is for default category objects
    private Object defaultKey;
    //Object pool for default category key
    private ObjectInstancePool defaultPool;
    //A lock array of object categories
    private ReentrantLock[] categoryLocks;

    //Refer to {@link BeeObjectSourceConfig#isForceRecycleBorrowedOnClose()}
    private boolean forceRecycleBorrowedOnClose;
    //Pool Monitor object
    private ObjectPoolMonitorVo poolMonitorVo;
    //A thread pool run servant tasks to search idle objects or create new objects for waiters
    private ThreadPoolExecutor servantService;

    //An interval time for below {@link scheduledService}to execute timed task
    private long timerCheckInterval;
    //A scheduled executor to scan timeout objects(idle timeout and hold timeout)
    private ScheduledThreadPoolExecutor scheduledService;
    //Check passed configuration
    private BeeObjectSourceConfig poolConfig;
    //A Hook to shut down pool when JVM exits
    private ObjectPoolHook exitHook;

    //***************************************************************************************************************//
    //                              1: Pool initializes                                                              //                                                                                  //
    //***************************************************************************************************************//
    //1.1: Pool initializes.
    public void init(BeeObjectSourceConfig config) throws Exception {
        if (config == null) throw new PoolInitializeFailedException("Configuration can't be null");
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
            throw new PoolInitializeFailedException("Object pool has initialized or in initializing");
        }
    }

    //1.2: Launch pool with check passed configuration
    private void startup(BeeObjectSourceConfig config) throws Exception {
        //step1: create default category pool and startup it.
        this.poolName = config.getPoolName();
        BeeObjectFactory objectFactory = config.getObjectFactory();
        this.defaultKey = objectFactory.getDefaultKey();
        this.defaultPool = new ObjectInstancePool(config, this);
        this.defaultPool.startup(poolName, defaultKey,
                config.getInitialSize(), config.isAsyncCreateInitObject());
        //put default category pool to map
        this.categoryPoolMap.put(defaultKey, defaultPool);

        //step2: Create locks
        this.forceRecycleBorrowedOnClose = config.isForceRecycleBorrowedOnClose();
        this.categoryMaxSize = config.getMaxKeySize();
        this.categoryLocks = new ReentrantLock[categoryMaxSize];
        for (int i = 0; i < categoryMaxSize; i++)
            categoryLocks[i] = new ReentrantLock();

        //step3: Create thread pool and schedule pool
        if (this.servantService != null) servantService.shutdownNow();
        if (this.scheduledService != null) scheduledService.shutdownNow();
        int coreThreadSize = Math.min(NCPU, categoryMaxSize);
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 15L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(categoryMaxSize), poolThreadFactory);
        this.scheduledService = new ScheduledThreadPoolExecutor(coreThreadSize, poolThreadFactory);
        this.timerCheckInterval = config.getTimerCheckInterval();
        this.scheduledService.scheduleWithFixedDelay(new TimeoutScanTask(defaultPool), timerCheckInterval,
                timerCheckInterval, MILLISECONDS);

        //step4: Create pool Hook
        if (this.exitHook == null) {
            this.exitHook = new ObjectPoolHook(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);
        }

        //step5: Create pool monitor object
        this.poolMonitorVo = new ObjectPoolMonitorVo(
                poolName,
                defaultPool.getPoolHostIP(),
                defaultPool.getPoolThreadId(),
                defaultPool.getPoolThreadName(),
                defaultPool.getPoolMode(),
                categoryMaxSize * config.getMaxActive());
    }

    //***************************************************************************************************************//
    //                              2: Pool Close(2)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    //2.1: Query pool state whether is closed
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //2.2: shutdown pool
    public void close() {
        final long parkTimeForRetryNs = defaultPool.getParkTimeForRetryNs();

        do {
            int poolStateCode = this.poolState;
            //exit if pool has shut down and in shutting down
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            //wait util completion of starting or clearing
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_CLEARING) {
                LockSupport.parkNanos(parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {
                //Start to shut down pool after cas successfully(current poolStateCode == POOL_NEW || poolStateCode == POOL_READY)
                Log.info("BeeOP({})Begin to shutdown", this.poolName);

                //shut down thread pools to avoid effect to category pools
                servantService.shutdown();
                scheduledService.shutdown();

                //shut down category pools
                for (ObjectInstancePool categoryPool : categoryPoolMap.values())
                    categoryPool.close(forceRecycleBorrowedOnClose);

                try {//remove Hook
                    Runtime.getRuntime().removeShutdownHook(this.exitHook);
                } catch (Throwable e) {
                    //do nothing
                }

                //mark pool as closed state
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
    //3.1: Physically closes objects in all category pools and remove them
    public void clear(boolean forceRecycleBorrowed) throws Exception {
        clear(forceRecycleBorrowed, false, null);
    }

    //3.1: Physically closes objects in all category pools and remove all category pools
    public void clear(boolean forceRecycleBorrowed, BeeObjectSourceConfig config) throws Exception {
        clear(forceRecycleBorrowed, true, config);
    }

    //3.3: Physically closes objects in all category pools
    private void clear(boolean forceRecycleBorrowed, boolean reinit, BeeObjectSourceConfig config) throws Exception {
        if (reinit && config == null)
            throw new BeeObjectSourceConfigException("Configuration can't be null");

        //clean pool after cas pool state success
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            try {
                //check the parameter configuration,if fail then exit method since here
                BeeObjectSourceConfig checkedConfig = null;
                if (reinit) checkedConfig = config.check();

                //clean sub pools one by one
                Log.info("BeeOP({})begin to remove all connections", this.poolName);
                for (ObjectInstancePool pool : categoryPoolMap.values())
                    pool.clear(forceRecycleBorrowed);
                Log.info("BeeOP({})completed to remove all connections", this.poolName);

                if (reinit) {
                    this.defaultKey = null;
                    this.defaultPool = null;
                    categoryPoolMap.clear();
                    this.poolConfig = checkedConfig;
                    Log.info("BeeOP({})start to reinitialize object pool", this.poolName);
                    this.startup(checkedConfig);//throws Exception only fail to create initial objects for default pool or fail to set default values
                    //note: if failed,this method may be recalled with correct configuration
                    Log.info("BeeOP({})completed to reinitialize object pool", this.poolName);
                }
            } finally {
                this.poolState = POOL_READY;//reset pool state to ready
            }
        } else {
            throw new PoolInClearingException("Object pool was closed or in cleaning");
        }
    }

    //***************************************************************************************************************//
    //                              4: Pool Monitoring and switch of log print(2)                                    //                                                                                  //
    //***************************************************************************************************************//
    //4.1: Enable or disable switch of runtime log print
    public void setPrintRuntimeLog(boolean switchIndicator) {
        for (ObjectInstancePool pool : categoryPoolMap.values()) {
            pool.setPrintRuntimeLog(switchIndicator);
        }
    }

    //4.2: get monitor object of this keyed pool
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int semaphoreWaitingSize = 0;
        int transferWaitingSize = 0;
        int idleSize = 0, usingSize = 0;
        for (ObjectInstancePool pool : categoryPoolMap.values()) {
            BeeObjectPoolMonitorVo monitorVo = pool.getPoolMonitorVo();
            idleSize += monitorVo.getIdleSize();
            usingSize += monitorVo.getBorrowedSize();
            semaphoreWaitingSize += monitorVo.getSemaphoreWaitingSize();
            transferWaitingSize += monitorVo.getTransferWaitingSize();
        }
        poolMonitorVo.setIdleSize(idleSize);
        poolMonitorVo.setBorrowedSize(usingSize);
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
            throw new ObjectGetForbiddenException("Object pool was not ready or closed");

        return defaultPool.getObjectHandle();
    }

    //2.2: gets an object from sub pool map to given key
    public BeeObjectHandle getObjectHandle(Object key) throws Exception {
        //1: Check inputted key
        this.checkKey(key);

        //2: Acquire an object from default category pool if is default key
        if (isDefaultKey(key)) return defaultPool.getObjectHandle();

        //3: Acquire an object from category pool exists in category pool map
        ObjectInstancePool categoryPool = categoryPoolMap.get(key);
        if (categoryPool != null) return categoryPool.getObjectHandle();

        //4: Throws exception when category size reach allowable max size
        if (categoryPoolMap.size() == this.categoryMaxSize)
            throw new ObjectKeyException("Object category capacity of pool has reach max size:" + categoryMaxSize);

        //5: Attempts to create category pool with given key
        ReentrantLock lock = categoryLocks[getArrayIndex(key.hashCode(), categoryMaxSize)];
        try {
            if (lock.tryLock(defaultPool.getMaxWaitNs(), TimeUnit.NANOSECONDS)) {
                try {
                    //repeat check category pool whether exists in category pool map
                    categoryPool = categoryPoolMap.get(key);
                    if (categoryPool == null) {
                        if (categoryPoolMap.size() == categoryMaxSize)
                            throw new ObjectKeyException("Object category capacity of pool has reach max size:" + categoryMaxSize);

                        //Create a category pool by clone
                        categoryPool = defaultPool.createByClone();
                        //Run category pool by async mode
                        categoryPool.startup(poolName, key, poolConfig.getInitialSize(), poolConfig.isAsyncCreateInitObject());
                        categoryPoolMap.put(key, categoryPool);
                        //Create time task to do timeout check on category pool
                        this.scheduledService.scheduleWithFixedDelay(new TimeoutScanTask(categoryPool), timerCheckInterval,
                                timerCheckInterval, MILLISECONDS);
                    }
                } finally {
                    lock.unlock();
                }

                //6: get handle from pool
                return categoryPool.getObjectHandle();
            } else {
                throw new ObjectGetTimeoutException("Waited timeout on lock of category(" + key + ")");
            }
        } catch (InterruptedException e) {
            throw new ObjectGetInterruptedException("An interruption occurred while waiting on lock of category(" + key + ")");
        }
    }

    //***************************************************************************************************************//
    //                                    6:Operation with Variety Key(10)                                           //
    //***************************************************************************************************************//
    public Object[] keys() {
        return this.categoryPoolMap.keySet().toArray();
    }

    public boolean exists(Object key) {
        return categoryPoolMap.containsKey(key);
    }

    public void clear(Object key) throws Exception {
        clear(key, false);
    }

    public void clear(Object key, boolean forceRecycleBorrowed) throws Exception {
        if (!getObjectInstancePool(key).clear(forceRecycleBorrowed))
            throw new PoolInClearingException("Target category(" + key + ") pool was closed or in cleaning");
    }

    public void deleteKey(Object key) throws Exception {
        deleteKey(key, false);
    }

    public void deleteKey(Object key, boolean forceRecycleBorrowed) throws Exception {
        if (!removeObjectInstancePool(key).clear(forceRecycleBorrowed))
            throw new PoolInClearingException("Target category(" + key + ") pool was closed or in cleaning");
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

    private void checkKey(Object key) throws Exception {
        if (key == null) throw new ObjectKeyException("Key can't be null or empty");
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Object pool was not ready or closed");
    }

    private ObjectInstancePool removeObjectInstancePool(Object key) throws Exception {
        checkKey(key);
        if (isDefaultKey(key)) throw new ObjectKeyException("Default key is forbidden to delete");

        ObjectInstancePool categoryPool = categoryPoolMap.remove(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Not found category pool with key(" + key + ")");
        return categoryPool;
    }

    private ObjectInstancePool getObjectInstancePool(Object key) throws Exception {
        checkKey(key);

        if (isDefaultKey(key)) return defaultPool;
        ObjectInstancePool categoryPool = categoryPoolMap.get(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Not found category pool with key(" + key + ")");
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
                Log.info("BeeOP({})Object pool hook is running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeOP({})Error occurred while pool hook running,cause:", this.pool.poolName, e);
            }
        }
    }
}
