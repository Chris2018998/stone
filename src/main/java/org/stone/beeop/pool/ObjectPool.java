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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beeop.BeeMethodExecutionLog.Type_Object_Get;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.NCPU;
import static org.stone.tools.CommonUtil.isNotBlank;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;
import static org.stone.tools.LogPrinter.getLogPrinter;

/**
 * A parent pool to manage some category pools by keys.
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

    //4: Flag of using fair mode on key
    private boolean useFairModeOfKey;
    //5: Flag of using thread local on key
    private boolean useThreadLocalOfKey;
    //6: Initialization size of objects during category pool starting
    private int initialSizeOfKey;
    //7: Max size of objects active in category pool
    private int maxActiveSizeOfKey;
    //8: Permit size of semaphore of category key
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
    //17: A internal container to cache methods execution logs
    private MethodExecutionLogCache<K> methodLogCache;

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
    //                                     1: Pooled objects getting(2+1)                                            //
    //***************************************************************************************************************//
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new BeeObjectSourcePoolRejectedException("Object pool was not ready or closed");

        if (this.collectMethodLogs) {
            BeeMethodExecutionLog<K> log = methodLogCache.beforeCall(this.defaultKey, Type_Object_Get, "KeyedObjectPool.getObjectHandle()", null);
            try {
                BeeObjectHandle<K, V> handle = defaultCategoryPool.getObjectHandle();
                methodLogCache.afterCall(handle, log);
                return handle;
            } catch (Throwable e) {
                methodLogCache.afterCall(e, log);
                throw e;
            }
        } else {
            return defaultCategoryPool.getObjectHandle();
        }
    }

    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        //1: if key is default,then call the get method (** Avoid twice read on volatile field ** )
        if (isDefaultKey(key)) return getObjectHandle();

        //2: Check parameter key(*** pool state check inside this method ***)
        this.checkKey(key);

        //3: Call internal get method
        if (this.collectMethodLogs) {
            BeeMethodExecutionLog<K> log = methodLogCache.beforeCall(key, Type_Object_Get, "KeyedObjectPool.getObjectHandle()", null);
            try {
                BeeObjectHandle<K, V> handle = getObjectHandleByInternal(key);
                methodLogCache.afterCall(handle, log);
                return handle;
            } catch (Throwable e) {
                methodLogCache.afterCall(e, log);
                throw e;
            }
        } else {
            return getObjectHandleByInternal(key);
        }
    }

    private BeeObjectHandle<K, V> getObjectHandleByInternal(K key) throws Exception {
        //1: Get category pool by key
        ObjectKeyCategoryPool<K, V> categoryPool = categoryPoolMap.get(key);

        //2:Call category pool to get pooled object if it exists
        if (categoryPool != null) return categoryPool.getObjectHandle();

        //3: Check key size weather reach max capacity
        if (categoryPoolMap.size() == this.maxKeySize)
            throw new ObjectKeyException("Object key size of pool has reach max size:" + maxKeySize);

        //4: Create category pool under object lock by synchronized keyword
        synchronized (key.toString().intern()) {
            categoryPool = categoryPoolMap.get(key);
            if (categoryPool == null) {
                if (categoryPoolMap.size() == maxKeySize)
                    throw new ObjectKeyException("Object key size of pool has reach max size:" + maxKeySize);

                //Create a new category pool by clone
                categoryPool = defaultCategoryPool.createByClone();

                //Schedule a timeout task on the new category pool(maybe blocked in creation)
                TimeoutObjectsClearTask<K, V> timeoutTask = new TimeoutObjectsClearTask<>(categoryPool);
                ScheduledFuture<?> future = this.scheduledService.scheduleWithFixedDelay(timeoutTask, timerCheckInterval,
                        timerCheckInterval, MILLISECONDS);

                //startup the category pool
                try {
                    categoryPool.startup(poolName, key, this.initialSizeOfKey, this.asyncCreateInitObjectsOfKey, logPrinter.isEnableLogOutput());
                } catch (Throwable e) {
                    //if failed,then remove the scheduled task
                    future.cancel(true);
                    this.scheduledService.remove(timeoutTask);//cancel the timeout task
                    throw e;
                }

                //put completed category pool into map
                categoryPoolMap.put(key, categoryPool);
            }

            // get handle from pool
            return categoryPool.getObjectHandle();
        }
    }

    //***************************************************************************************************************//
    //                                     2: Pooled keys maintenance(8+0)                                           //
    //***************************************************************************************************************//
    public int keySize() {
        return categoryPoolMap.size();
    }

    public boolean exists(K key) {
        return categoryPoolMap.containsKey(key);
    }

    public boolean suspendKey(K key) throws Exception {
        return getObjectInstancePool(key).suspendKey();
    }

    public boolean resumeKey(K key) throws Exception {
        return getObjectInstancePool(key).resumeKey();
    }

    public void clearObjects(K key) throws Exception {
        clearObjects(key, false);
    }

    public void clearObjects(K key, boolean forceRecycleBorrowed) throws Exception {
        if (!getObjectInstancePool(key).restart(forceRecycleBorrowed))
            throw new BeeObjectSourcePoolRestartedException("Target category(" + key + ") Pool has been closed or is restarting");
    }

    public void deleteKey(K key) throws Exception {
        deleteKey(key, false);
    }

    public void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        removeObjectInstancePool(key).close(forceRecycleBorrowed);
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

    public void start(BeeObjectSourceConfig<K, V> config) throws Exception {//4
        if (config == null) throw new BeeObjectSourcePoolStartedException("Object source configuration can't be null");
        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {
            try {
                startupInternal(config.check());
                this.poolState = POOL_READY;
            } catch (Throwable e) {
                this.poolState = POOL_NEW;//reset to new state when fail
                throw e;
            }
        } else {
            throw new BeeObjectSourcePoolStartedException("Pool has already initialized or in initializing");
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
            Class<?>[] objectProxyClasses = ProxyClassGenerator.genProxyClassWithInterface(null, interfaces, config.getMethodNameListOnListen());
            objectProxyClassConstructor = objectProxyClasses[0].getDeclaredConstructors()[0];
        }

        //step3: Create default category pool
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

        //step8: Create method execution log cache and schedule a timed task on it
        this.collectMethodLogs = config.isEnableMethodExecutionLogCache();
        this.methodLogCache = new MethodExecutionLogCache<>();
        this.methodLogCache.initCache(this.poolName, config.getMethodExecutionLogCacheSize(),
                config.getSlowObjectGetThreshold(), config.getSlowObjectExecutionThreshold(), config.getMethodExecutionListener());
        this.scheduledService.scheduleWithFixedDelay(new TimeoutMethodLogsClearTask<>(
                        this, config.getMethodExecutionLogTimeout(), this.methodLogCache),
                config.getIntervalOfClearTimeoutExecutionLogs(), config.getIntervalOfClearTimeoutExecutionLogs(), MILLISECONDS);

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
            } finally {
                this.poolState = POOL_READY;//reset pool state to ready
            }
        } else {
            throw new BeeObjectSourcePoolRestartedException("Object Pool has been closed or is restarting");
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
        if (this.methodLogCache != null) {
            this.methodLogCache.clear(BeeMethodExecutionLog.Type_All);
            this.methodLogCache = null;
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
    //                                      4: Pool Log Print(2+0)                                                   //
    //***************************************************************************************************************//
    public void enableLogPrint(boolean enable) {
        this.logPrinter = getLogPrinter(ObjectPool.class, enable);
        for (ObjectKeyCategoryPool<K, V> pool : categoryPoolMap.values()) {
            pool.enableLogPrint(enable);
        }
    }

    public void enableLogPrint(K key, boolean enable) throws Exception {
        getObjectInstancePool(key).enableLogPrint(enable);
    }

    //***************************************************************************************************************//
    //                                     5: Pool Monitoring(2+0)                                                   //
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
    //                                     6: Pool blocking interrupts(2+0)                                          //
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
    //                                     7: method execution logs                                              //
    //***************************************************************************************************************//
    public void enableMethodExecutionLogCache(boolean enable) {

    }

    public void setMethodExecutionListener(BeeMethodExecutionListener<K> listener) {

    }

    public List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(int type) {
        return null;
    }


    public List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(K key, int type) {
        return null;
    }

    public List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(K key, int type) {
        return null;
    }

    //***************************************************************************************************************//
    //                                  8: MBean Registration (0+2)                                                  //
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
    //                                  9: Private methods and friendly methods (5)                                  //                                                                                  //
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
            throw new BeeObjectSourcePoolRejectedException("Object Internal pool was not ready or closed");
    }

    private ObjectKeyCategoryPool<K, V> removeObjectInstancePool(K key) throws Exception {
        checkKey(key);
        if (isDefaultKey(key)) throw new ObjectKeyException("Default key is forbidden to delete");

        ObjectKeyCategoryPool<K, V> categoryPool = categoryPoolMap.remove(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Not found category pool with key(" + key + ")");
        return categoryPool;
    }

    private ObjectKeyCategoryPool<K, V> getObjectInstancePool(K key) throws Exception {
        checkKey(key);

        if (isDefaultKey(key)) return defaultCategoryPool;
        ObjectKeyCategoryPool<K, V> categoryPool = categoryPoolMap.get(key);
        if (categoryPool == null)
            throw new ObjectKeyNotExistsException("Not found category pool with key(" + key + ")");
        return categoryPool;
    }

    //***************************************************************************************************************//
    //                                  10: Internal classes(4)                                                      //                                                                                  //
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
                                                    MethodExecutionLogCache<K> methodExecutionLogCache) implements Runnable {

        public void run() {
            try {
                methodExecutionLogCache.clearTimeout(methodExecutionLogTimeout);
            } catch (Throwable e) {
                pool.logPrinter.warn("BeeOP({})-an exception occurred while scanning timeout method logs", this.pool.poolName, e);
            }
        }
    }
}
