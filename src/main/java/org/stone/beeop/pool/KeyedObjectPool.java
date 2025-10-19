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

import org.stone.beeop.*;
import org.stone.beeop.pool.exception.*;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;
import org.stone.tools.logger.LogPrinter;
import org.stone.tools.logger.LogPrinterFactory;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.POOL_READY;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.NCPU;
import static org.stone.tools.CommonUtil.getArrayIndex;

/**
 * keyed object pool implementation,which is a manager pool maintains a default category pool and some category pools
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public final class KeyedObjectPool<K, V> implements BeeKeyedObjectPool<K, V> {
    private static final AtomicIntegerFieldUpdater<KeyedObjectPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(KeyedObjectPool.class, "poolState");
    private final ConcurrentHashMap<K, ObjectInstancePool<K, V>> categoryPoolMap = new ConcurrentHashMap<>(1);
    String poolName;
    private LogPrinter Log = LogPrinterFactory.getLogPrinter(KeyedObjectPool.class);
    private volatile int poolState;
    //max size of object category
    private int categoryMaxSize;
    //An Array of locks to create category pools and launch them
    private ReentrantLock[] categoryLocks;
    //Key of default category type
    private K defaultKey;
    //Object creation size during sub pools initialization
    private int initialSize;
    //create mode of object initialization
    private boolean asyncCreateInitObject;
    //Refer to {@link BeeObjectSourceConfig#isForceRecycleBorrowedOnClose()}
    private boolean forceRecycleBorrowedOnClose;
    //Refer to {@link BeeObjectSourceConfig#isForceShutdownThreadPoolOnClose()}
    private boolean forceShutdownThreadPoolOnClose;
    //Object pool for default category key
    private ObjectInstancePool<K, V> defaultPool;
    //Pool Monitor object
    private ObjectPoolMonitorVo poolMonitorVo;
    //A thread pool run servant tasks to search idle objects or create new objects for waiters
    private ThreadPoolExecutor servantService;
    //An interval time for below {@link scheduledService}to execute timed task
    private long timerCheckInterval;
    //A scheduled executor to scan timeout objects(idle timeout and hold timeout)
    private ScheduledThreadPoolExecutor scheduledService;
    //A Hook to shut down pool when JVM exits
    private ObjectPoolHook<K, V> exitHook;

    //***************************************************************************************************************//
    //                              1: Pool initializes                                                              //                                                                                  //
    //***************************************************************************************************************//
    //1.1: Pool initializes.
    public void start(BeeObjectSourceConfig<K, V> config) throws Exception {
        if (config == null) throw new PoolInitializeFailedException("Configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {
            try {
                startup(config.check());
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
    private void startup(BeeObjectSourceConfig<K, V> config) throws Exception {
        //step1: set log print flag
        this.Log = LogPrinterFactory.getLogPrinter(config.isPrintRuntimeLogs() ? KeyedObjectPool.class : null);

        //step2: generate Object Proxy class
        Constructor<?> objectProxyClassConstructor = null;
        Class<?>[] interfaces = config.getObjectInterfaces();
        if (interfaces != null) {
            Class<?> objectProxyClass = ProxyClassGenerator.genProxyClassWithInterface(interfaces);
            objectProxyClassConstructor = objectProxyClass.getDeclaredConstructors()[0];
        }

        //step3: copy some field to local
        this.poolName = config.getPoolName();
        this.initialSize = config.getInitialSize();
        this.asyncCreateInitObject = config.isAsyncCreateInitObject();
        this.forceRecycleBorrowedOnClose = config.isForceRecycleBorrowedOnClose();
        this.forceShutdownThreadPoolOnClose = config.isForceShutdownThreadPoolOnClose();

        //step4: create default pool and launch it
        this.defaultPool = new ObjectInstancePool<>(config, this, objectProxyClassConstructor);
        BeeObjectFactory<K, V> objectFactory = config.getObjectFactory();
        this.defaultKey = objectFactory.getDefaultKey();
        this.defaultPool.startup(poolName, defaultKey, this.initialSize, this.asyncCreateInitObject, Log.isOutputLogs());
        this.categoryPoolMap.put(defaultKey, defaultPool);

        //step5: Creates Locks
        this.categoryMaxSize = config.getMaxKeySize();
        this.categoryLocks = new ReentrantLock[categoryMaxSize];
        for (int i = 0; i < categoryMaxSize; i++)
            categoryLocks[i] = new ReentrantLock();

        //step6: Closes existed thread execution pool and task pool
        if (this.scheduledService != null) {
            if (forceShutdownThreadPoolOnClose) {
                servantService.shutdownNow();
                scheduledService.shutdownNow();
            } else {
                servantService.shutdown();
                scheduledService.shutdown();
                long parkTimeForRetryNs = defaultPool.getParkTimeForRetryNs();
                while (!servantService.isTerminated() || !scheduledService.isTerminated()) {
                    LockSupport.parkNanos(parkTimeForRetryNs);
                }
            }
        }

        //step7: Create new thread execution pool and task schedule pool
        int coreThreadSize = Math.min(NCPU, categoryMaxSize);
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 15L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(categoryMaxSize), poolThreadFactory);
        this.servantService.allowCoreThreadTimeOut(true);

        this.timerCheckInterval = config.getIntervalToClearTimeout();
        this.scheduledService = new ScheduledThreadPoolExecutor(coreThreadSize, poolThreadFactory);
        this.scheduledService.setMaximumPoolSize(coreThreadSize);
        this.scheduledService.allowCoreThreadTimeOut(true);
        this.scheduledService.setKeepAliveTime(15L, TimeUnit.SECONDS);
        this.scheduledService.scheduleWithFixedDelay(new TimeoutScanTask<>(defaultPool), timerCheckInterval, timerCheckInterval, MILLISECONDS);

        //step8: Create pool Hook
        if (this.exitHook == null) {
            this.exitHook = new ObjectPoolHook<>(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);
        }

        //step9: Create pool monitor object
        this.poolMonitorVo = new ObjectPoolMonitorVo(
                poolName,
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

    public boolean isReady() {
        return this.poolState == POOL_READY;
    }

    //2.2: shutdown pool
    public void close() {
        final long parkTimeForRetryNs = defaultPool.getParkTimeForRetryNs();

        do {
            int poolStateCode = this.poolState;
            //exit if pool has shut down or in shutting down
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            //wait util completion of starting or clearing
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_RESTARTING) {
                LockSupport.parkNanos(parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//state must be one of(POOL_NEW,POOL_READY)
                Log.info("BeeOP({})Begin to shutdown", this.poolName);

                if (this.forceShutdownThreadPoolOnClose) {
                    servantService.shutdownNow();
                    scheduledService.shutdownNow();
                } else {
                    servantService.shutdown();
                    scheduledService.shutdown();
                    while (!servantService.isTerminated() || !scheduledService.isTerminated()) {
                        LockSupport.parkNanos(parkTimeForRetryNs);
                    }
                }

                //shut down category pools
                for (ObjectInstancePool<K, V> categoryPool : categoryPoolMap.values())
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
    //                              3: Pool Clean(3)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    //3.0: Physically closes objects in all category pools and remove them
    public void restart(boolean forceRecycleBorrowed) throws Exception {
        restart(forceRecycleBorrowed, false, null);
    }

    //3.1: Physically closes objects in all category pools and remove all category pools
    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {
        restart(forceRecycleBorrowed, true, config);
    }

    //3.3: Physically closes objects in all category pools
    private void restart(boolean forceRecycleBorrowed, boolean reinit, BeeObjectSourceConfig<K, V> config) throws Exception {
        if (reinit && config == null)
            throw new BeeObjectSourceConfigException("Configuration can't be null");

        //clean pool after cas pool state success
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_RESTARTING)) {
            try {
                //check the parameter configuration,if fail then exit method since here
                BeeObjectSourceConfig<K, V> checkedConfig = null;
                if (reinit) checkedConfig = config.check();

                //clean sub pools one by one
                Log.info("BeeOP({})begin to remove all pooled objects", this.poolName);
                for (ObjectInstancePool<K, V> pool : categoryPoolMap.values())
                    pool.restart(forceRecycleBorrowed);
                Log.info("BeeOP({})completed to remove all pooled objects", this.poolName);

                if (reinit) {
                    this.defaultKey = null;
                    this.defaultPool = null;
                    categoryPoolMap.clear();
                    Log.info("BeeOP({})start to reinitialize object pool", this.poolName);
                    this.startup(checkedConfig);//note: if fail to startup,you can adjust configuration and retry this method
                    Log.info("BeeOP({})completed to reinitialize object pool", this.poolName);
                }
            } finally {
                this.poolState = POOL_READY;//reset pool state to ready
            }
        } else {
            throw new PoolInClearingException("Object Pool has been closed or is restarting");
        }
    }

    //***************************************************************************************************************//
    //                              4: Pool Monitoring and switch of log print(2)                                    //                                                                                  //
    //***************************************************************************************************************//
    //4.1: Enable or disable switch of runtime log print
    public void enableLogPrint(boolean enable) {
        this.Log = LogPrinterFactory.getLogPrinter(enable ? KeyedObjectPool.class : null);
        for (ObjectInstancePool<K, V> pool : categoryPoolMap.values()) {
            pool.setPrintRuntimeLog(enable);
        }
    }

    //4.2: get monitor object of this keyed pool
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int keySize = 0;
        int semaphoreWaitingSize = 0;
        int transferWaitingSize = 0;
        int idleSize = 0, usingSize = 0;
        for (ObjectInstancePool<K, V> pool : categoryPoolMap.values()) {
            BeeObjectPoolMonitorVo subMonitorVo = pool.getPoolMonitorVo();
            keySize++;

            idleSize += subMonitorVo.getIdleSize();
            usingSize += subMonitorVo.getBorrowedSize();
            semaphoreWaitingSize += subMonitorVo.getSemaphoreWaitingSize();
            transferWaitingSize += subMonitorVo.getTransferWaitingSize();
        }
        poolMonitorVo.setKeySize(keySize);
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
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Object Internal pool was not ready or closed");

        return defaultPool.getObjectHandle();
    }

    //2.2: gets an object from sub pool map to given key
    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        //1: Check inputted key
        this.checkKey(key);

        //2: Acquire an object from default category pool if is default key
        if (isDefaultKey(key)) return defaultPool.getObjectHandle();

        //3: Acquire an object from category pool exists in category pool map
        ObjectInstancePool<K, V> categoryPool = categoryPoolMap.get(key);
        if (categoryPool != null) return categoryPool.getObjectHandle();

        //4: Throws exception when category size reach max size
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
                            throw new ObjectKeyException("Object keys size reached max capacity:" + categoryMaxSize);

                        //Create a category pool by clone
                        categoryPool = defaultPool.createByClone();
                        //Run category pool by async mode
                        categoryPool.startup(poolName, key, this.initialSize, this.asyncCreateInitObject, Log.isOutputLogs());
                        categoryPoolMap.put(key, categoryPool);
                        //Create time task to do timeout check on category pool
                        this.scheduledService.scheduleWithFixedDelay(new TimeoutScanTask<>(categoryPool), timerCheckInterval,
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
    //                                    6:Operation with Variety Key(8)                                            //
    //***************************************************************************************************************//
    public int keySize() {
        return categoryPoolMap.size();
    }

    public boolean exists(K key) {
        return categoryPoolMap.containsKey(key);
    }

    public void reset(K key) throws Exception {
        reset(key, false);
    }

    public void reset(K key, boolean forceRecycleBorrowed) throws Exception {
        if (!getObjectInstancePool(key).restart(forceRecycleBorrowed))
            throw new PoolInClearingException("Target category(" + key + ") Pool has been closed or is restarting");
    }

    public void deleteKey(K key) throws Exception {
        deleteKey(key, false);
    }

    public void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        if (!removeObjectInstancePool(key).restart(forceRecycleBorrowed))
            throw new PoolInClearingException("Target category(" + key + ") Pool has been closed or is restarting");
    }

    public boolean isEnabledLogPrint(K key) throws Exception {
        return getObjectInstancePool(key).isPrintRuntimeLog();
    }

    public void enableLogPrint(K key, boolean enable) throws Exception {
        getObjectInstancePool(key).setPrintRuntimeLog(enable);
    }


    public BeeObjectPoolMonitorVo getMonitorVo(K key) throws Exception {
        return getObjectInstancePool(key).getPoolMonitorVo();
    }

    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        return getObjectInstancePool(key).interruptWaitingThreads();
    }

    public List<Thread> interruptWaitingThreads() throws Exception {
        List<Thread> threadList = new LinkedList<>();
        for (ObjectInstancePool<K, V> instance : categoryPoolMap.values())
            threadList.addAll(instance.interruptWaitingThreads());
        return threadList;
    }

    /**
     * Queries logs collector in whether in being enabled.
     *
     * @return boolean true is enabled,false is disabled
     */
    public boolean isEnabledObjectCallLogManager() {
        return false;//@todo
    }

    /**
     * A switch to enable or disable configured log collector in pool.
     *
     * @param enable is true that enable, false is disabled
     */
    public void enableObjectCallLogManager(boolean enable) {

    }

    //***************************************************************************************************************//
    //                7: Private methods and friendly methods (4)                                                    //                                                                                  //
    //***************************************************************************************************************//
    void submitServantTask(Runnable task) {
        this.servantService.submit(task);
    }

    private boolean isDefaultKey(K key) {
        return defaultKey == key || defaultKey.equals(key);
    }

    private void checkKey(K key) throws Exception {
        if (key == null) throw new ObjectKeyException("Key can't be null");
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Object Internal pool was not ready or closed");
    }

    private ObjectInstancePool<K, V> removeObjectInstancePool(K key) throws Exception {
        checkKey(key);
        if (isDefaultKey(key)) throw new ObjectKeyException("Default key is forbidden to delete");

        ObjectInstancePool<K, V> categoryPool = categoryPoolMap.remove(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Not found category pool with key(" + key + ")");
        return categoryPool;
    }

    private ObjectInstancePool<K, V> getObjectInstancePool(K key) throws Exception {
        checkKey(key);

        if (isDefaultKey(key)) return defaultPool;
        ObjectInstancePool<K, V> categoryPool = categoryPoolMap.get(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Not found category pool with key(" + key + ")");
        return categoryPool;
    }

    //***************************************************************************************************************//
    //                      9: Internal classes(3)                                                                  //                                                                                  //
    //***************************************************************************************************************//
    private record TimeoutScanTask<K, V>(ObjectInstancePool<K, V> pool) implements Runnable {

        public void run() {
            try {
                this.pool.closeIdleTimeout();
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    private record PoolThreadFactory(String threadName) implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        }
    }

    private static class ObjectPoolHook<K, V> extends Thread {
        private final KeyedObjectPool<K, V> pool;

        ObjectPoolHook(KeyedObjectPool<K, V> pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                pool.Log.info("BeeOP({})Object pool hook is running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                pool.Log.error("BeeOP({})Error occurred while pool hook running,cause:", this.pool.poolName, e);
            }
        }
    }
}
