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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beeop.BeeMethodLog.*;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.NCPU;
import static org.stone.tools.CommonUtil.isNotBlank;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;
import static org.stone.tools.LogPrinter.getLogPrinter;

/**
 * Object key pool impl.
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public final class ObjectPool<K, V> implements BeeObjectPool<K, V>, ObjectPoolMXBean {
    private static final VarHandle PoolStateUpd;

    static {
        try {
            PoolStateUpd = MethodHandles.lookup().findVarHandle(ObjectPool.class, "poolState", int.class);
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    private final ConcurrentHashMap<K, PooledObjectBucket<K, V>> objectBucketMap = new ConcurrentHashMap<>(1);

    //1: Pool name
    private String poolName;
    //2: Pool state
    private volatile int poolState;
    //3: key counter(decreasing)
    private AtomicInteger keyCounter;

    //4: Initialization size of objects during category pool starting
    private int initialSizeOfKey;
    //5: Creation mode of initial objects for category pools
    private boolean asyncCreateInitObjectsOfKey;

    //6: Policy flag to recycle borrowed objects when pool close
    private boolean forceRecycleBorrowedOnClose;
    //7: A global thread executor pool to run tasks to help to get objects for borrowers in wait queue
    private ThreadPoolExecutor servantService;
    //8: A global thread executor pool to run timed tasks(timeout objects clear,timeout method logs clear)
    private ScheduledThreadPoolExecutor scheduledService;

    //9: Flag of pool logs cache
    private boolean enabledLogCache;
    //10: Log cache of pool
    private ObjectPoolLogCache<K> poolLogCache;
    //11: Timeout value of method logs
    private long methodLogsTimeout;
    //12: Time interval of task to clear timeout logs of pool
    private long intervalOfMethodLogsClearTask;
    //13: Handle of scheduled task to clear logs
    private ScheduledFuture<?> poolLogsScheduledFuture;

    //14: Pool name on MBean registered
    private String poolNameOfRegisteredMBean;
    //15: JVM Hook to shut down pool
    private ObjectPoolHook<K, V> jvmExitHook;
    //16: Log printer of key pool
    private LogPrinter logPrinter = DefaultLogPrinter;

    //***************************************default key and default object bucket ***********************************//
    //17: Default Key
    private K defaultKey;
    //18: Default bucket to default key
    private PooledObjectBucket<K, V> defaultObjectBucket;

    //***************************************************************************************************************//
    //                                     1: Pool start(1+1)                                                        //
    //***************************************************************************************************************//
    public void start(BeeObjectSourceConfig<K, V> config) throws Exception {
        if (config == null)
            throw new BeeObjectSourcePoolStartedFailureException("Object source configuration can't be null");

        if (PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING)) {
            try {
                startupInternal(config.check());
                this.poolState = POOL_READY;
            } catch (Throwable e) {
                this.poolState = POOL_NEW;//reset to new state when fail
                throw new BeeObjectSourcePoolStartedFailureException("Object source pool started failure", e);
            }
        } else {
            throw new BeeObjectSourcePoolStartedFailureException("Object source pool is starting up or has already started");
        }
    }

    //Internal method to start pool
    private void startupInternal(BeeObjectSourceConfig<K, V> config) throws Exception {
        //step1: set log printer
        this.poolName = config.getPoolName();
        this.logPrinter = getLogPrinter(ObjectPool.class, config.isPrintRuntimeLogs());

        //step2: Generate proxy class(@deprecated)
        Constructor<?> objectProxyClassConstructor = null;
        Class<?>[] interfaces = config.getObjectInterfaces();
        if (interfaces != null) {
            Class<?>[] objectProxyClasses = ObjectProxyGenerator.genProxyClassWithInterface(null, interfaces, config.getObjectMethodNameList());
            objectProxyClassConstructor = objectProxyClasses[0].getDeclaredConstructors()[0];
        }

        //step3: Create pool schedule executor(** schedule a task on default category pool, the task can interrupt possible block during startup **)
        int maxKeySize = config.getMaxKeySize();
        int coreThreadSizeOfScheduledThreadPool = Math.min(NCPU, (maxKeySize << 1) + 1);//1 is for clear timeout logs of key pool
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        this.scheduledService = new ScheduledThreadPoolExecutor(coreThreadSizeOfScheduledThreadPool, poolThreadFactory);
        this.scheduledService.setMaximumPoolSize(coreThreadSizeOfScheduledThreadPool);
        this.scheduledService.allowCoreThreadTimeOut(true);
        this.scheduledService.setKeepAliveTime(10L, TimeUnit.SECONDS);

        //step4: Create Pool logs cache
        this.enabledLogCache = config.isEnableLogCache();
        this.poolLogCache = new ObjectPoolLogCache<>();
        this.poolLogCache.init(this.poolName, config.getLogCacheSize(), config.getLogListener());
        this.poolLogCache.setSlowThreshold(Type_Pool_Log, config.getSlowGetThreshold());

        //step5: Create default bucket and start it
        this.keyCounter = new AtomicInteger(maxKeySize);
        this.initialSizeOfKey = config.getInitialSize();
        this.asyncCreateInitObjectsOfKey = config.isAsyncCreateInitObjects();
        this.forceRecycleBorrowedOnClose = config.isForceRecycleBorrowedOnClose();
        this.defaultObjectBucket = new PooledObjectBucket<>(this, config, objectProxyClassConstructor, this.scheduledService);
        this.defaultKey = config.getObjectFactory().getDefaultKey();
        this.defaultObjectBucket = this.startKeyCategoryPool(defaultKey, System.currentTimeMillis());

        //step6: Create thread executor pool to run servant tasks to get pooled objects
        int threadPoolCoreThreadSize = Math.min(NCPU, maxKeySize);
        this.servantService = new ThreadPoolExecutor(threadPoolCoreThreadSize, threadPoolCoreThreadSize, 10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(maxKeySize), poolThreadFactory);
        this.servantService.allowCoreThreadTimeOut(true);

        //step7: Create a timed task to clear timeout logs of pool
        this.methodLogsTimeout = config.getLogTimeout();
        this.intervalOfMethodLogsClearTask = config.getIntervalOfClearTimeoutLogs();
        if (enabledLogCache) {
            this.poolLogsScheduledFuture = this.scheduledService.scheduleWithFixedDelay(new TimeoutMethodLogsOfPoolClearTask<>(this.poolLogCache, methodLogsTimeout, this),
                    intervalOfMethodLogsClearTask, intervalOfMethodLogsClearTask, MILLISECONDS);
        }

        //step8: Register configuration object and pool to JMX Platform
        if (config.isRegisterMbeans()) registerMBeans(config);

        //step9: Register JVM Hook
        if (config.isRegisterJvmHook()) {
            this.jvmExitHook = new ObjectPoolHook<>(this);
            Runtime.getRuntime().addShutdownHook(this.jvmExitHook);
        }
    }

    //***************************************************************************************************************//
    //                                     2: Pool restart(2+1)                                                      //
    //***************************************************************************************************************//
    public void restart(boolean forceRecycleBorrowed) throws Exception {
        restart(forceRecycleBorrowed, false, null);
    }

    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {
        restart(forceRecycleBorrowed, true, config);
    }

    private void restart(boolean forceRecycleBorrowed, boolean reinit, BeeObjectSourceConfig<K, V> config) throws Exception {
        if (reinit && config == null)
            throw new BeeObjectSourcePoolRestartedFailureException("Object source configuration can't be null");

        int poolState = this.poolState;
        boolean hasRunToStartupInternal = false;
        if ((poolState == POOL_READY || poolState == POOL_RESTART_FAILED) && PoolStateUpd.compareAndSet(this, poolState, POOL_RESTARTING)) {
            try {
                if (reinit) {//restart with new configuration
                    logPrinter.info("BeeOP({})-begin to restart pool with new configuration", this.poolName);
                    BeeObjectSourceConfig<K, V> checkedConfig = config.check();

                    //1: close category pools and remove them, reset some fields
                    this.clearPoolInternalMembers(forceRecycleBorrowed);

                    //2: startup pool
                    hasRunToStartupInternal = true;
                    this.startupInternal(checkedConfig);
                    logPrinter.info("BeeOP({})-pool has restarted up", this.poolName);
                } else {//Only Clear all category pools
                    logPrinter.info("BeeOP({})-begin to restart", this.poolName);
                    for (PooledObjectBucket<K, V> pool : objectBucketMap.values())
                        pool.restart(forceRecycleBorrowed);
                    logPrinter.info("BeeOP({})-pool has restarted up", this.poolName);
                }

                this.poolState = POOL_READY;//reset pool state to ready
            } catch (Throwable e) {
                logPrinter.error("BeeOP({})-restarted failure", this.poolName, e);
                this.poolState = hasRunToStartupInternal ? POOL_RESTART_FAILED : POOL_READY;
                throw new BeeObjectSourcePoolRestartedFailureException("Object source pool restarted failure", e);
            }
        } else {
            throw new BeeObjectSourcePoolRestartedFailureException("Object source pool has been closed or is restarting");
        }
    }

    //***************************************************************************************************************//
    //                                     3: Pooled objects get(2+2)                                                //
    //***************************************************************************************************************//
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new BeeObjectSourcePoolNotReadyException("Object pool was not ready");

        return defaultObjectBucket.getObjectHandle(0L);
    }

    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        //1: call getObjectHandle() when key is default
        if (isDefaultKey(key)) return getObjectHandle();

        //2: Check key(*** pool state check inside this method ***)
        this.checkKey(key);

        //3: Get category pool with key,if category pool exists,then call it to get pooled object
        PooledObjectBucket<K, V> categoryPool = objectBucketMap.get(key);
        if (categoryPool != null) return categoryPool.getObjectHandle(0L);

        //4: Check key size of pool before add new key to pool
        if (this.keyCounter.get() == 0)//no remained capacity
            throw new BeePooledObjectKeyException("Pooled key size has reach max capacity");

        //5: add new key to pool with 'synchronized' keyword
        long startTime = System.currentTimeMillis();
        synchronized (key.toString().intern()) {
            categoryPool = objectBucketMap.get(key);
            if (categoryPool == null) categoryPool = startKeyCategoryPool(key, startTime);
        }//synchronized code snippet

        //6: Attempt to get a pooled object from the started pool
        return categoryPool.getObjectHandle(startTime);
    }

    //Attempt to start category pool by key
    private PooledObjectBucket<K, V> startKeyCategoryPool(K key, long startTime) throws Exception {
        if (this.enabledLogCache) {
            BeeMethodLog<K> log = this.poolLogCache.beforeCall(startTime, key, Type_Pool_Log, "ObjectPool.startKeyCategoryPool", null);
            try {
                PooledObjectBucket<K, V> categoryPool = this.createKeyCategoryPool(key);
                this.poolLogCache.afterCall(System.currentTimeMillis(), categoryPool, log);
                return categoryPool;
            } catch (Throwable e) {
                this.poolLogCache.afterCall(System.currentTimeMillis(), e, log);
                throw e;
            }
        } else {
            return this.createKeyCategoryPool(key);
        }
    }

    //Attempt to create category pool and start it
    private PooledObjectBucket<K, V> createKeyCategoryPool(K key) throws Exception {
        //1: decrement key capacity
        int cur;
        do {
            cur = this.keyCounter.get();
            if (cur == 0)
                throw new BeePooledObjectKeyException("Pooled key size has reach max capacity");
        } while (!keyCounter.compareAndSet(cur, cur - 1));

        //2: Create a category pool(Object instances pool) by clone
        PooledObjectBucket<K, V> categoryPool = defaultObjectBucket.createByClone();

        //3: Startup the created category pool()
        try {
            categoryPool.startup(key, this.initialSizeOfKey, this.asyncCreateInitObjectsOfKey, logPrinter.isEnableLogOutput());
        } catch (Throwable e) {
            keyCounter.incrementAndGet();
            throw e;
        }

        //4: put the started pool to concurrent map(global)
        objectBucketMap.put(key, categoryPool);

        //5: return the started pool to get objects
        return categoryPool;
    }

    //***************************************************************************************************************//
    //                                     4: Pooled keys maintenance(8+0)                                           //
    //***************************************************************************************************************//
    public int keySize() {
        return objectBucketMap.size();
    }

    public boolean existsKey(K key) {
        return objectBucketMap.containsKey(key);
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
            throw new BeePooledObjectKeyException("Target category(" + key + ") pool has been closed or is clearing");
    }

    public boolean deleteKey(K key) throws Exception {
        return deleteKey(key, false);
    }

    public boolean deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        PooledObjectBucket<K, V> deletedCategoryPool = removeObjectInstancePool(key);
        if (deletedCategoryPool == null) return false;
        this.keyCounter.decrementAndGet();//decrement one number value
        deletedCategoryPool.close(forceRecycleBorrowed);
        return true;
    }

    //***************************************************************************************************************//
    //                                    5: Pool Monitor (3+0)                                                      //
    //***************************************************************************************************************//
    public String toString() {
        return getPoolStateDesc(this.poolState);
    }

    public boolean isClosed() {//1
        return this.poolState == POOL_CLOSED;
    }

    public void enableLogPrinter(boolean enable) {
        this.logPrinter = getLogPrinter(ObjectPool.class, enable);
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo(boolean includeKeys) throws Exception {
        ObjectPoolMonitorVo monitorVo = new ObjectPoolMonitorVo(
                poolName,
                this.poolState,
                this.logPrinter.isEnableLogOutput(),
                this.enabledLogCache);
        if (includeKeys) {
            for (PooledObjectBucket<K, V> pool : objectBucketMap.values()) {
                PooledObjectBucketMonitorVo keyMonitorVo = pool.getKeyMonitorVo();
                monitorVo.pubKeyMonitorVo(keyMonitorVo.getKeyName(), keyMonitorVo);
            }
        }
        return monitorVo;
    }

    public BeeObjectKeyMonitorVo getKeyMonitorVo(K key) throws Exception {
        return getObjectInstancePool(key).getKeyMonitorVo();
    }

    public void enableLogPrinter(K key, boolean enable) throws Exception {
        getObjectInstancePool(key).enableLogPrint(enable);
    }

    //***************************************************************************************************************//
    //                                    6: Pool Close and suspend (3+0)                                            //
    //***************************************************************************************************************//
    public boolean suspend() {//2
        return PoolStateUpd.compareAndSet(this, POOL_READY, POOL_SUSPENDED);
    }

    public boolean resume() {//3
        return PoolStateUpd.compareAndSet(this, POOL_SUSPENDED, POOL_READY);
    }

    public void close() {
        final long parkTimeForRetryNs = defaultObjectBucket != null ? defaultObjectBucket.getParkTimeForRetryNs() : MILLISECONDS.toNanos(10L);

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
                this.clearPoolInternalMembers(this.forceRecycleBorrowedOnClose);

                //2: Set pool state to closed
                this.poolState = POOL_CLOSED;

                logPrinter.info("BeeOP({})-has shutdown", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    private void clearPoolInternalMembers(boolean forceRecycleBorrowed) {
        //NOTE: Safe shut down on pool,make sure pool threads dead before clear other fields

        //1: Clear log cache
        if (this.poolLogCache != null) poolLogCache.clearLogs(Type_Pool_Log);

        //2: Shut down schedule executor pool
        if (this.scheduledService != null) {
            if (poolLogsScheduledFuture != null) {
                poolLogsScheduledFuture.cancel(true);
                poolLogsScheduledFuture = null;
            }

            this.scheduledService.shutdownNow();
            this.scheduledService.getQueue().clear();
            this.scheduledService = null;
        }

        //3: Shut down servant thread executor pool
        if (this.servantService != null) {
            this.servantService.shutdownNow();
            this.servantService.getQueue().clear();
            this.servantService = null;
        }

        //4: Clear all category pools
        for (PooledObjectBucket<K, V> categoryPool : this.objectBucketMap.values())
            categoryPool.close(forceRecycleBorrowed);
        this.objectBucketMap.clear();
        this.keyCounter = null;
        this.defaultKey = null;
        this.defaultObjectBucket = null;

        //5: Unregister MBeans
        if (isNotBlank(this.poolNameOfRegisteredMBean))
            this.unregisterMBeans();

        //6: unregister Pool hook
        if (this.jvmExitHook != null) {//this method call is from pool close
            try {//remove Hook
                Runtime.getRuntime().removeShutdownHook(this.jvmExitHook);
            } catch (Throwable e) {
                //do nothing
            } finally {
                this.jvmExitHook = null;
            }
        }
    }

    //***************************************************************************************************************//
    //                                    6: Pool blocking interrupts(2+0)                                           //
    //***************************************************************************************************************//
    public List<Thread> interruptWaitingThreads() {
        List<Thread> threadList = new LinkedList<>();
        for (PooledObjectBucket<K, V> instance : objectBucketMap.values())
            threadList.addAll(instance.interruptWaitingThreads());
        return threadList;
    }

    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        return getObjectInstancePool(key).interruptWaitingThreads();
    }

    //***************************************************************************************************************//
    //                                    7: method execution logs(4+0)                                              //
    //***************************************************************************************************************//
    public void enableLogCache(boolean enable) {
        if (this.enabledLogCache != enable) {
            this.enabledLogCache = enable;
            if (enable) {
                this.poolLogsScheduledFuture = this.scheduledService.scheduleWithFixedDelay(new TimeoutMethodLogsOfPoolClearTask<>(this.poolLogCache, methodLogsTimeout, this),
                        intervalOfMethodLogsClearTask, intervalOfMethodLogsClearTask, MILLISECONDS);
            } else if (this.poolLogsScheduledFuture != null) {
                this.poolLogCache.clearLogs(Type_Pool_Log);
                this.poolLogsScheduledFuture.cancel(true);
                this.poolLogsScheduledFuture = null;
            }
        }
    }

    public void changeLogListener(BeeMethodLogListener<K> listener) {
        this.poolLogCache.setLogListener(listener);
    }

    public void clearPoolLogs() {
        this.poolLogCache.clearTimeoutLogs(0L);
    }

    public List<BeeMethodLog<K>> getPoolLogs() {
        return this.poolLogCache.getLogs(Type_Pool_Log);
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
    public BeeObjectPoolMonitorVo getPoolMonitorVo() throws Exception {
        return this.getPoolMonitorVo(false);
    }

    public String[] getKeyNames() {
        List<String> keyNameList = new LinkedList<>();
        for (PooledObjectBucket<K, V> categoryPool : this.objectBucketMap.values()) {
            keyNameList.add(categoryPool.getKeyName());
        }
        return keyNameList.toArray(new String[0]);
    }

    public void enableKeyLogPrinterByName(String keyName, boolean enable) {
        for (PooledObjectBucket<K, V> categoryPool : this.objectBucketMap.values()) {
            if (categoryPool.getKeyName().equals(keyName)) {
                categoryPool.enableLogPrint(enable);
                break;
            }
        }
    }

    public void enableKeyLogCacheByName(String keyName, boolean enable) {
        for (PooledObjectBucket<K, V> categoryPool : this.objectBucketMap.values()) {
            if (categoryPool.getKeyName().equals(keyName)) {
                categoryPool.enableLogCache(enable);
                break;
            }
        }
    }

    public BeeObjectKeyMonitorVo getKeyMonitorVoByName(String keyName) throws Exception {
        for (PooledObjectBucket<K, V> categoryPool : this.objectBucketMap.values()) {
            if (categoryPool.getKeyName().equals(keyName)) {
                return categoryPool.getKeyMonitorVo();
            }
        }
        return null;
    }

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
            logPrinter.warn("BeeOP({})-failed to unregister a MBean with name:{}", this.poolName, poolMBeanName, e);
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

    private PooledObjectBucket<K, V> removeObjectInstancePool(K key) throws Exception {
        checkKey(key);
        if (isDefaultKey(key)) throw new BeePooledObjectKeyException("Default key is forbidden to delete");
        return objectBucketMap.remove(key);
    }

    private PooledObjectBucket<K, V> getObjectInstancePool(K key) throws Exception {
        checkKey(key);

        if (isDefaultKey(key)) return defaultObjectBucket;
        PooledObjectBucket<K, V> categoryPool = objectBucketMap.get(key);
        if (categoryPool == null)
            throw new BeePooledObjectKeyNotFoundException("Not found category pool with key(" + key + ")");
        return categoryPool;
    }

    //***************************************************************************************************************//
    //                                    11: Internal classes(3)                                                    //                                                                                  //
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

    private record TimeoutMethodLogsOfPoolClearTask<K, V>(ObjectPoolLogCache<K> newKeysLogCache,
                                                          long timeout,
                                                          ObjectPool<K, V> objectPool) implements Runnable {

        public void run() {
            try {
                newKeysLogCache.clearTimeoutLogs(timeout);
            } catch (Throwable e) {
                objectPool.logPrinter.warn("BeeOP({})-an exception occurred while scanning timeout method logs", this.objectPool.poolName, e);
            }
        }
    }
}
