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

import jakarta.annotation.Nonnull;
import org.stone.beeop.*;
import org.stone.beeop.exception.*;
import org.stone.tools.BeanUtil;
import org.stone.tools.LogPrinter;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beeop.BeeMethodLog.*;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.NCPU;
import static org.stone.tools.CommonUtil.isNotBlank;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;
import static org.stone.tools.LogPrinter.getLogPrinter;

/**
 * A parent pool manage some category pools by keys
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public final class ObjectPool<K, V> implements BeeObjectPool<K, V>, ObjectPoolMXBean<K> {
    private static final AtomicIntegerFieldUpdater<ObjectPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectPool.class, "poolState");
    private final ConcurrentHashMap<K, ObjectKeyCategoryPool<K, V>> categoryPoolMap = new ConcurrentHashMap<>(1);

    //1: Pool name
    private String poolName;
    //2: Pool state
    private volatile int poolState;
    //3: Max size of pooled keys
    private int maxKeySize;
    //4: key counter
    private AtomicInteger keyCounter;

    //5: Flag of using fair mode on key
    private boolean useFairModeOfKey;
    //6: Flag of using thread local on key
    private boolean useThreadLocalOfKey;
    //7: Initialization size of objects during category pool starting
    private int initialSizeOfKey;
    //8: Max size of objects active in category pool
    private int maxActiveSizeOfKey;
    //9: Permit size of semaphore of category key
    private int semaphoreSizeOfKey;
    //9: Creation mode of initial objects for category pools
    private boolean asyncCreateInitObjectsOfKey;

    //10: Policy flag to recycle borrowed objects when pool close
    private boolean forceRecycleBorrowedOnClose;
    //11: Policy flag to shut down internal thread pools when pool close
    private boolean forceShutdownThreadPoolOnClose;

    //12: A threads pool to search idle objects or create new objects for waiters
    private ThreadPoolExecutor servantService;
    //13: An interval time to clear timeout objects from category pools
    private long timerCheckInterval;
    //14: Schedule thread pool to execute time clear tasks
    private ScheduledThreadPoolExecutor scheduledService;

    //15: Pool name on MBean registered
    private String poolNameOfRegisteredMBean;

    //16: Flag to collect method execution logs
    private boolean collectMethodLogs;
    //17: A internal container to cache new keys
    private ObjectPoolLogCache<K> newKeysLogCache;

    //18: Log printer of key pool
    private LogPrinter logPrinter = DefaultLogPrinter;
    //19: JVM Hook to shut down pool
    private ObjectPoolHook<K, V> exitHook;

    //***************************************Some fields for default category pool ***********************************//
    //20: Default Key
    private K defaultKey;
    //21: Default category pool
    private ObjectKeyCategoryPool<K, V> defaultCategoryPool;

    //***************************************************************************************************************//
    //                                     1: Pooled objects get(2+1)                                                //
    //***************************************************************************************************************//
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new BeeObjectSourcePoolNotReadyException("Object pool was not ready");

        return defaultCategoryPool.getObjectHandle(0L);
    }

    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        //1: if key is default,then call the get method
        if (isDefaultKey(key)) return getObjectHandle();

        //2: Check key(*** pool state check inside this method ***)
        this.checkKey(key);

        //3: Get category pool with key,if category pool exists,then call it to get pooled object
        ObjectKeyCategoryPool<K, V> categoryPool = categoryPoolMap.get(key);
        if (categoryPool != null) return categoryPool.getObjectHandle(0L);

        //4: Check key size of pool before add new key to pool
        if (this.keyCounter.get() == this.maxKeySize)
            throw new BeePooledObjectKeyException("Pooled key size has reach max capacity:" + maxKeySize);

        //5: attempt to add key to pool
        long startTime = System.currentTimeMillis();
        synchronized (key.toString().intern()) {
            categoryPool = categoryPoolMap.get(key);
            if (categoryPool == null) {
                if (this.collectMethodLogs) {
                    BeeMethodLog<K> log = this.newKeysLogCache.beforeCall(startTime, key, Type_Pool_Log, "ObjectPool.getObjectHandle", null);
                    try {
                        categoryPool = this.createObjectKeyCategoryPool(key);
                        this.newKeysLogCache.afterCall(System.currentTimeMillis(), categoryPool, log);
                    } catch (Throwable e) {
                        this.newKeysLogCache.afterCall(System.currentTimeMillis(), e, log);
                        throw e;
                    }
                } else {
                    categoryPool = this.createObjectKeyCategoryPool(key);
                }
            }
        }//synchronized code snippet

        //6: Attempt tto get a pooled object from the started pool
        return categoryPool.getObjectHandle(startTime);
    }

    private ObjectKeyCategoryPool<K, V> createObjectKeyCategoryPool(K key) throws Exception {
        //1: increase atomic counter
        int cur;
        do {
            cur = this.keyCounter.get();
            if (cur == this.maxKeySize)
                throw new BeePooledObjectKeyException("Pooled key size has reach max capacity:" + maxKeySize);
        } while (!keyCounter.compareAndSet(cur, cur + 1));

        //2: clone a new instance with default pool
        ObjectKeyCategoryPool<K, V> categoryPool = defaultCategoryPool.createByClone();

        //3: Schedule a timeout task on the new category pool(maybe blocked in creation)
        TimeoutObjectsClearTask<K, V> timeoutTask = new TimeoutObjectsClearTask<>(categoryPool);
        ScheduledFuture<?> future = this.scheduledService.scheduleWithFixedDelay(timeoutTask, timerCheckInterval,
                timerCheckInterval, MILLISECONDS);

        try {
            //4: Attempt to start up the created pool
            categoryPool.startup(poolName, key, this.initialSizeOfKey, this.asyncCreateInitObjectsOfKey, logPrinter.isEnableLogOutput());

            //5: put the started pool to concurrent map
            categoryPoolMap.put(key, categoryPool);
            return categoryPool;
        } catch (Throwable e) {
            //6: remove scheduled task when pool failed to start up
            keyCounter.decrementAndGet();//decrease
            future.cancel(true);
            this.scheduledService.remove(timeoutTask);
            throw e;
        }
    }

    //***************************************************************************************************************//
    //                                     2: Pooled keys maintenance(8+0)                                           //
    //***************************************************************************************************************//
    public int keySize() {
        return categoryPoolMap.size();
    }

    public boolean existsKey(K key) {
        return categoryPoolMap.containsKey(key);
    }

    public boolean suspendKey(K key) throws Exception {
        return getObjectInstancePool(key).suspendKey();
    }

    public boolean resumeKey(K key) throws Exception {
        return getObjectInstancePool(key).resumeKey();
    }

    public void clearKeyObjects(K key) throws Exception {
        clearKeyObjects(key, false);
    }

    public void clearKeyObjects(K key, boolean forceRecycleBorrowed) throws Exception {
        if (!getObjectInstancePool(key).restart(forceRecycleBorrowed))
            throw new BeeObjectSourcePoolRestartedFailureException("Target category(" + key + ") Pool has been closed or is restarting");
    }

    public boolean deleteKey(K key) throws Exception {
        return deleteKey(key, false);
    }

    public boolean deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        ObjectKeyCategoryPool<K, V> deletedCategoryPool = removeObjectInstancePool(key);
        if (deletedCategoryPool == null) return false;
        this.keyCounter.decrementAndGet();//decrement one number value
        deletedCategoryPool.close(forceRecycleBorrowed);
        return true;
    }

    //***************************************************************************************************************//
    //                                    3: Pool maintenance (7+2)                                                  //
    //***************************************************************************************************************//
    public boolean isClosed() {//1
        return this.poolState == POOL_CLOSED;
    }

    public boolean suspendPool() {//2
        return PoolStateUpd.compareAndSet(this, POOL_READY, POOL_SUSPENDED);
    }

    public boolean resumePool() {//3
        return PoolStateUpd.compareAndSet(this, POOL_SUSPENDED, POOL_READY);
    }

    public void start(BeeObjectSourceConfig<K, V> config) throws Exception {
        if (config == null) throw new BeeObjectSourceConfigException("Object source configuration can't be null");

        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {
            try {
                startupInternal(config.check());
                this.poolState = POOL_READY;
            } catch (Throwable e) {
                this.poolState = POOL_NEW;//reset to new state when fail
                if (e instanceof BeeObjectSourcePoolException) {
                    throw (BeeObjectSourcePoolException) e;
                } else {
                    throw new BeeObjectSourcePoolStartedFailureException("Object source pool started failure", e);
                }
            }
        } else {
            throw new BeeObjectSourcePoolStartedFailureException("Object source pool is starting or already started up");
        }
    }

    private void startupInternal(BeeObjectSourceConfig<K, V> config) throws Exception {
        //step1: set log printer first
        this.poolName = config.getPoolName();
        this.logPrinter = getLogPrinter(ObjectPool.class, config.isPrintRuntimeLogs());

        //step2: Create Proxy classes{@link org.stone.beeop#getObject()}
        Constructor<?> objectProxyClassConstructor = null;
        Class<?>[] interfaces = config.getObjectInterfaces();
        if (interfaces != null) {
            Class<?>[] objectProxyClasses = ObjectProxyGenerator.genProxyClassWithInterface(null, interfaces, config.getObjectMethodNameList());
            objectProxyClassConstructor = objectProxyClasses[0].getDeclaredConstructors()[0];
        }

        //step3: Create default category pool
        this.keyCounter = new AtomicInteger();
        this.defaultCategoryPool = new ObjectKeyCategoryPool<>(this, config, objectProxyClassConstructor);

        //step4: Create scheduled thread pool before default category pool starts
        this.maxKeySize = config.getMaxKeySize();
        int coreThreadSize = Math.min(NCPU, maxKeySize + 1);//1 is to run method execution log clear task
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        this.timerCheckInterval = config.getIntervalOfClearTimeout();
        this.scheduledService = new ScheduledThreadPoolExecutor(coreThreadSize, poolThreadFactory);
        this.scheduledService.setMaximumPoolSize(coreThreadSize);
        this.scheduledService.allowCoreThreadTimeOut(true);
        this.scheduledService.setKeepAliveTime(10L, TimeUnit.SECONDS);

        //step5: launch a time task on default category pool before it start up(*** need add some check in task guarantee it initialization completed ***)
        TimeoutObjectsClearTask<K, V> timeoutTask = new TimeoutObjectsClearTask<>(defaultCategoryPool);
        ScheduledFuture<?> future = this.scheduledService.scheduleWithFixedDelay(timeoutTask, timerCheckInterval, timerCheckInterval, MILLISECONDS);

        //step6: Launch the default category pool
        this.defaultKey = config.getObjectFactory().getDefaultKey();
        this.initialSizeOfKey = config.getInitialSize();
        this.asyncCreateInitObjectsOfKey = config.isAsyncCreateInitObjects();
        try {
            this.defaultCategoryPool.startup(poolName, defaultKey, this.initialSizeOfKey, this.asyncCreateInitObjectsOfKey, logPrinter.isEnableLogOutput());
        } catch (Throwable e) {
            //remove the scheduled task when fails to startup
            future.cancel(true);
            scheduledService.remove(timeoutTask);
            throw e;
        }

        //step7: put default category pool into map
        this.categoryPoolMap.put(defaultKey, defaultCategoryPool);//put the default category pool to concurrent map
        this.keyCounter.set(1);

        //step8: Create method execution log cache and schedule a timed task on it
        this.collectMethodLogs = config.isEnableLogCache();
        this.newKeysLogCache = new ObjectPoolLogCache<>();
        this.newKeysLogCache.setSlowThreshold(Type_Pool_Log, config.getSlowGetThreshold());
        this.newKeysLogCache.init(this.poolName,
                1,
                config.getLogCacheSize(),
                config.getLogListener());

        this.scheduledService.scheduleWithFixedDelay(new TimeoutMethodLogsClearTask<>(
                        this, config.getLogTimeout(), this.newKeysLogCache),
                config.getIntervalOfClearTimeoutLogs(), config.getIntervalOfClearTimeoutLogs(), MILLISECONDS);

        //step9: Create a thread pool executor to get objects for sleeping waiters
        this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(maxKeySize), poolThreadFactory);
        this.servantService.allowCoreThreadTimeOut(true);

        //step10: Copy some configuration items to pool local
        this.useFairModeOfKey = config.isFairMode();
        this.useThreadLocalOfKey = config.isUseThreadLocal();
        this.maxActiveSizeOfKey = config.getMaxActive();
        this.semaphoreSizeOfKey = config.getSemaphoreSize();
        this.forceRecycleBorrowedOnClose = config.isForceRecycleBorrowedOnClose();
        this.forceShutdownThreadPoolOnClose = config.isForceShutdownThreadPoolOnClose();

        //step11: Register MBeans rely on configuration
        if (config.isRegisterMbeans()) registerMBeans(config);

        //step12: Create a hook and register it,if not existed
        if (this.exitHook == null) {
            this.exitHook = new ObjectPoolHook<>(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);
        }
    }

    //Restart pool with last used configuration
    public void restart(boolean forceRecycleBorrowed) throws Exception {//5
        restart(forceRecycleBorrowed, false, null);
    }

    //Restart pool with a new configuration
    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {//6
        restart(forceRecycleBorrowed, true, config);
    }

    //Internal method to restart pool
    private void restart(boolean forceRecycleBorrowed, boolean reinit, BeeObjectSourceConfig<K, V> config) throws Exception {
        if (reinit && config == null)
            throw new BeeObjectSourceConfigException("Object source configuration can't be null");

        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_RESTARTING)) {
            try {
                if (reinit) {//restart with new configuration
                    BeeObjectSourceConfig<K, V> checkedConfig = config.check();
                    logPrinter.info("BeeOP({})-begin to restart pool with new configuration", this.poolName);
                    //1: Clear some fields of pool
                    this.clearPoolFields(false, forceRecycleBorrowed);

                    //2: startup pool
                    this.startupInternal(checkedConfig);
                    logPrinter.info("BeeOP({})-finished pool restart", this.poolName);
                } else {//Only Clear all category pools
                    logPrinter.info("BeeOP({})-begin to restart key category pools", this.poolName);
                    for (ObjectKeyCategoryPool<K, V> pool : categoryPoolMap.values())
                        pool.restart(forceRecycleBorrowed);
                    logPrinter.info("BeeOP({})-completed to restart key category pools", this.poolName);
                }
            } catch (Throwable e) {
                logPrinter.error("BeeOP({})-restarted failure", this.poolName, e);
                if (e instanceof BeeObjectSourcePoolException) {
                    throw (BeeObjectSourcePoolException) e;
                } else {
                    throw new BeeObjectSourcePoolRestartedFailureException("Object source pool restarted failure", e);
                }
            } finally {
                this.poolState = POOL_READY;//reset pool state to ready
            }
        } else {
            throw new BeeObjectSourcePoolRestartedFailureException("Object source pool has been closed or is restarting");
        }
    }

    public void close() {//7
        final long parkTimeForRetryNs = defaultCategoryPool.getParkTimeForRetryNs();

        do {
            int poolStateCode = this.poolState;
            //exit if pool has shut down or in shutting down
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            //wait util completion of starting or clearing
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_RESTARTING) {
                LockSupport.parkNanos(parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//state must be one of(POOL_NEW,POOL_READY)
                logPrinter.info("BeeOP({})-begin to shutdown", this.poolName);

                //1: Clear some fields of pool
                this.clearPoolFields(true, this.forceRecycleBorrowedOnClose);

                //2: Set pool state to closed
                this.poolState = POOL_CLOSED;

                logPrinter.info("BeeOP({})-has shutdown", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    private void clearPoolFields(boolean isCloseCall, boolean forceRecycleBorrowed) {
        //NOTE: Safe shut down on pool,make sure pool threads dead before clear other fields

        //1: Shut down schedule service
        if (this.scheduledService != null) {
            this.scheduledService.getQueue().clear();
            this.scheduledService.shutdownNow();
            this.scheduledService = null;
        }

        //2: Shut down servant thread executor pool
        if (this.servantService != null) {
            this.servantService.getQueue().clear();
            this.servantService.shutdownNow();
            this.servantService = null;
        }

        //3: Clear default key and its pool
        this.defaultKey = null;
        this.defaultCategoryPool = null;

        //4: Shut down all category pools and clear the map of them
        for (ObjectKeyCategoryPool<K, V> categoryPool : this.categoryPoolMap.values())
            categoryPool.close(forceRecycleBorrowed);
        this.categoryPoolMap.clear();

        //5: Clear log cache of method execution
        if (this.newKeysLogCache != null) {
            this.newKeysLogCache.clearTimeoutLogs(0L);//clear all logs
            this.newKeysLogCache = null;
        }

        //6: Unregister MBeans
        if (isNotBlank(this.poolNameOfRegisteredMBean))
            this.unregisterMBeans();

        //7: unregister Pool hook
        if (isCloseCall) {
            try {//remove Hook
                Runtime.getRuntime().removeShutdownHook(this.exitHook);
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    //***************************************************************************************************************//
    //                                    4: Pool Log Printer(2+0)                                                   //
    //***************************************************************************************************************//
    public void enableLogPrinter(boolean enable) {
        this.logPrinter = getLogPrinter(ObjectPool.class, enable);
        for (ObjectKeyCategoryPool<K, V> pool : categoryPoolMap.values()) {
            pool.enableLogPrint(enable);
        }
    }

    public void enableLogPrinter(K key, boolean enable) throws Exception {
        getObjectInstancePool(key).enableLogPrint(enable);
    }

    //***************************************************************************************************************//
    //                                    5: Pool Monitoring(2+0)                                                    //
    //***************************************************************************************************************//
    public BeeObjectPoolMonitorVo<K> getPoolMonitorVo(boolean keyMonitor) {
        ObjectPoolMonitorVo<K> monitorVo = new ObjectPoolMonitorVo<>(
                poolName,
                this.useFairModeOfKey,
                this.useThreadLocalOfKey,
                this.maxKeySize,
                this.maxActiveSizeOfKey,
                this.semaphoreSizeOfKey,
                this.poolState,
                this.logPrinter.isEnableLogOutput(),
                this.collectMethodLogs);

        if (keyMonitor) {
            for (ObjectKeyCategoryPool<K, V> pool : categoryPoolMap.values()) {
                BeeObjectKeyMonitorVo<K> keyMonitorVo = pool.getKeyMonitorVo();
                monitorVo.pubKeyMonitorVo(keyMonitorVo.getKey(), keyMonitorVo);
            }
        }
        return monitorVo;
    }

    public BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) throws Exception {
        return getObjectInstancePool(key).getKeyMonitorVo();
    }

    //***************************************************************************************************************//
    //                                    6: Pool blocking interrupts(2+0)                                          //
    //***************************************************************************************************************//
    public List<Thread> interruptWaitingThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (ObjectKeyCategoryPool<K, V> instance : categoryPoolMap.values())
            threadList.addAll(instance.interruptWaitingThreads());
        return threadList;
    }

    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        return getObjectInstancePool(key).interruptWaitingThreads();
    }

    //***************************************************************************************************************//
    //                                    7: method execution logs(4+0)                                             //
    //***************************************************************************************************************//
    public void enableLogCache(boolean enable) {
        this.collectMethodLogs = enable;
    }

    public void changeLogListener(BeeMethodLogListener<K> listener) {
        this.newKeysLogCache.setLogListener(listener);
    }

    public void clearPoolLogs() {
        this.newKeysLogCache.clearTimeoutLogs(0L);
    }

    public List<BeeMethodLog<K>> getPoolLogs() {
        return this.newKeysLogCache.getLogs(Type_Pool_Log);
    }

    //***************************************************************************************************************//
    //                                    8: Key method logs(6+0)                                                    //
    //***************************************************************************************************************//
    public void enableLogCache(K key, boolean enable) throws Exception {
        getObjectInstancePool(key).enableLogCache(enable);
    }

    public void changeLogListener(K key, BeeMethodLogListener<K> listener) throws Exception {
        getObjectInstancePool(key).setLogListener(listener);
    }

    public void clearKeyLogs(K key) throws Exception {
        getObjectInstancePool(key).clearLogs(Type_Key_Log);
    }

    public List<BeeMethodLog<K>> getKeyLogs(K key) throws Exception {
        return getObjectInstancePool(key).getLogs(Type_Key_Log);
    }

    public void clearKeyObjectLogs(K key) throws Exception {
        getObjectInstancePool(key).clearLogs(Type_Object_Log);
    }

    public List<BeeMethodLog<K>> getKeyObjectLogs(K key) throws Exception {
        return getObjectInstancePool(key).getLogs(Type_Object_Log);
    }

    //***************************************************************************************************************//
    //                                    9: MBean Registration (0+2)                                                //
    //***************************************************************************************************************//
    private void registerMBeans(BeeObjectSourceConfig<K, V> poolConfig) {
        String configMBeanName = String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolName);
        try {
            BeanUtil.registerMBean(configMBeanName, poolConfig);
        } catch (Throwable e) {
            logPrinter.warn("BeeOP({})-failed to register a MBean with name:{}", this.poolName, configMBeanName, e);
        }

        String poolMBeanName = String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", this.poolName);
        try {
            BeanUtil.registerMBean(poolMBeanName, this);
        } catch (Throwable e) {
            logPrinter.warn("BeeOP({})-failed to register a MBean with name:{}", this.poolName, poolMBeanName, e);
        }

        //Record pool name
        this.poolNameOfRegisteredMBean = this.poolName;
    }

    private void unregisterMBeans() {
        String configMBeanName = String.format("org.stone.beeop.BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolNameOfRegisteredMBean);
        try {
            BeanUtil.unregisterMBean(configMBeanName);
        } catch (Throwable e) {
            logPrinter.warn("BeeOP({})-failed to unregister a MBean with name:{}", this.poolName, configMBeanName, e);
        }

        String poolMBeanName = String.format("org.stone.beeop.pool.KeyedObjectPool:type=BeeOP(%s)", this.poolNameOfRegisteredMBean);
        try {
            BeanUtil.unregisterMBean(poolMBeanName);
        } catch (Throwable e) {
            logPrinter.warn("BeeOP({})-failed to unregister a MBean with name:{}", this.poolName, poolNameOfRegisteredMBean, e);
        }

        //clear pool name for MBean
        this.poolNameOfRegisteredMBean = null;
    }

    //***************************************************************************************************************//
    //                                    10: Private methods and friendly methods (5)                               //                                                                                  //
    //***************************************************************************************************************//
    void submitServantTask(Runnable task) {
        this.servantService.submit(task);
    }

    private boolean isDefaultKey(K key) {
        return defaultKey == key || defaultKey.equals(key);
    }

    private void checkKey(K key) throws Exception {
        if (key == null) throw new BeePooledObjectKeyException("Key can't be null");
        if (this.poolState != POOL_READY)
            throw new BeeObjectSourcePoolNotReadyException("Object Internal pool was not ready or closed");
    }

    private ObjectKeyCategoryPool<K, V> removeObjectInstancePool(K key) throws Exception {
        checkKey(key);
        if (isDefaultKey(key)) throw new BeePooledObjectKeyException("Default key is forbidden to delete");
        return categoryPoolMap.remove(key);
    }

    private ObjectKeyCategoryPool<K, V> getObjectInstancePool(K key) throws Exception {
        checkKey(key);

        if (isDefaultKey(key)) return defaultCategoryPool;
        ObjectKeyCategoryPool<K, V> categoryPool = categoryPoolMap.get(key);
        if (categoryPool == null)
            throw new BeePooledObjectKeyNotFoundException("Not found category pool with key(" + key + ")");
        return categoryPool;
    }

    //***************************************************************************************************************//
    //                                    11: Internal classes(4)                                                    //                                                                                  //
    //***************************************************************************************************************//
    private record PoolThreadFactory(String threadName) implements ThreadFactory {

        @Override
        public Thread newThread(@Nonnull Runnable runnable) {
            return new Thread(runnable, threadName);
        }
    }

    private static class ObjectPoolHook<K, V> extends Thread {
        private final ObjectPool<K, V> pool;

        ObjectPoolHook(ObjectPool<K, V> pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                pool.logPrinter.info("BeeOP({})-object pool hook is running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                pool.logPrinter.error("BeeOP({})-error occurred while pool hook running,cause:", this.pool.poolName, e);
            }
        }
    }

    private record TimeoutObjectsClearTask<K, V>(ObjectKeyCategoryPool<K, V> pool) implements Runnable {

        public void run() {
            try {
                this.pool.closeIdleTimeout();
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    private record TimeoutMethodLogsClearTask<K, V>(ObjectPool<K, V> pool, long methodExecutionLogTimeout,
                                                    ObjectPoolLogCache<K> methodExecutionLogCache) implements Runnable {

        public void run() {
            try {
                methodExecutionLogCache.clearTimeoutLogs(methodExecutionLogTimeout);
            } catch (Throwable e) {
                pool.logPrinter.warn("BeeOP({})-an exception occurred while scanning timeout method logs", this.pool.poolName, e);
            }
        }
    }
}
