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
 * A parent pool manage some category pools by keys
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

    private final ConcurrentHashMap<K, ObjectKeyCategoryPool<K, V>> categoryPoolMap = new ConcurrentHashMap<>(1);

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
    private boolean collectPoolLogs;
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
    private ObjectPoolHook<K, V> exitHook;
    //16: Log printer of key pool
    private LogPrinter logPrinter = DefaultLogPrinter;

    //***************************************Some fields for default category pool ***********************************//
    //17: Default Key
    private K defaultKey;
    //18: Default category pool
    private ObjectKeyCategoryPool<K, V> defaultCategoryPool;

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

    //method call to start up key pool
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

        //step3: Create pool schedule executor
        int maxKeySize = config.getMaxKeySize();
        int coreThreadSizeOfScheduledThreadPool = Math.min(NCPU, (maxKeySize << 1) + 1);//1 is for clear timeout logs of key pool
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);
        this.scheduledService = new ScheduledThreadPoolExecutor(coreThreadSizeOfScheduledThreadPool, poolThreadFactory);
        this.scheduledService.setMaximumPoolSize(coreThreadSizeOfScheduledThreadPool);
        this.scheduledService.allowCoreThreadTimeOut(true);
        this.scheduledService.setKeepAliveTime(10L, TimeUnit.SECONDS);

        //step4: Create category Pool for default key by configuration
        this.forceRecycleBorrowedOnClose = config.isForceRecycleBorrowedOnClose();
        this.defaultCategoryPool = new ObjectKeyCategoryPool<>(this, config, objectProxyClassConstructor, this.scheduledService);

        //step5: Start the default category pool
        this.initialSizeOfKey = config.getInitialSize();
        this.asyncCreateInitObjectsOfKey = config.isAsyncCreateInitObjects();
        this.defaultKey = config.getObjectFactory().getDefaultKey();
        this.defaultCategoryPool.startup(defaultKey, this.initialSizeOfKey, this.asyncCreateInitObjectsOfKey, logPrinter.isEnableLogOutput());
        //step6: put the created default pool to map

        this.categoryPoolMap.put(defaultKey, defaultCategoryPool);
        this.keyCounter = new AtomicInteger(maxKeySize - 1);//remained key capacity: max key size -1

        //step7: Create thread executor pool to add objects to category pools
        int threadPoolCoreThreadSize = Math.min(NCPU, maxKeySize);
        this.servantService = new ThreadPoolExecutor(threadPoolCoreThreadSize, threadPoolCoreThreadSize, 10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(maxKeySize), poolThreadFactory);
        this.servantService.allowCoreThreadTimeOut(true);

        //step8: Create a method log cache for key pool
        this.poolLogCache = new ObjectPoolLogCache<>(this.poolName, config.getLogCacheSize(), config.getLogListener());
        this.poolLogCache.setSlowThreshold(Type_Pool_Log, config.getSlowGetThreshold());
        this.methodLogsTimeout = config.getLogTimeout();
        this.intervalOfMethodLogsClearTask = config.getIntervalOfClearTimeoutLogs();
        this.collectPoolLogs = config.isEnableLogCache();
        if (collectPoolLogs) {
            this.poolLogsScheduledFuture = this.scheduledService.scheduleWithFixedDelay(new TimeoutMethodLogsOfPoolClearTask<>(this.poolLogCache, methodLogsTimeout, this),
                    intervalOfMethodLogsClearTask, intervalOfMethodLogsClearTask, MILLISECONDS);
        }

        //step9: Register MBeans
        if (config.isRegisterMbeans()) registerMBeans(config);

        //step10: Register JVM Hook
        if (config.isRegisterJvmHook()) {
            this.exitHook = new ObjectPoolHook<>(this);
            Runtime.getRuntime().addShutdownHook(this.exitHook);
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
            throw new BeeObjectSourceConfigException("Object source configuration can't be null");

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
                    for (ObjectKeyCategoryPool<K, V> pool : categoryPoolMap.values())
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
    //                                     3: Pooled objects get(2+1)                                                //
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
        if (this.keyCounter.get() == 0)//no remained capacity
            throw new BeePooledObjectKeyException("Pooled key size has reach max capacity");

        //5: attempt to add key to pool
        long startTime = System.currentTimeMillis();
        synchronized (key.toString().intern()) {
            categoryPool = categoryPoolMap.get(key);
            if (categoryPool == null) {
                if (this.collectPoolLogs) {
                    BeeMethodLog<K> log = this.poolLogCache.beforeCall(startTime, key, Type_Pool_Log, "ObjectPool.getObjectHandle", null);
                    try {
                        categoryPool = this.createObjectKeyCategoryPool(key);
                        this.poolLogCache.afterCall(System.currentTimeMillis(), categoryPool, log);
                    } catch (Throwable e) {
                        this.poolLogCache.afterCall(System.currentTimeMillis(), e, log);
                        throw e;
                    }
                } else {
                    categoryPool = this.createObjectKeyCategoryPool(key);
                }
            }
        }//synchronized code snippet

        //6: Attempt to get a pooled object from the started pool
        return categoryPool.getObjectHandle(startTime);
    }


    private ObjectKeyCategoryPool<K, V> createObjectKeyCategoryPool(K key) throws Exception {
        //1: decrement key capacity
        int cur;
        do {
            cur = this.keyCounter.get();
            if (cur == 0)
                throw new BeePooledObjectKeyException("Pooled key size has reach max capacity");
        } while (!keyCounter.compareAndSet(cur, cur - 1));

        //2: Create a category pool(Object instances pool) by clone
        ObjectKeyCategoryPool<K, V> categoryPool = defaultCategoryPool.createByClone();

        //3: Startup the created category pool()
        try {
            categoryPool.startup(key, this.initialSizeOfKey, this.asyncCreateInitObjectsOfKey, logPrinter.isEnableLogOutput());
        } catch (Throwable e) {
            keyCounter.incrementAndGet();
            throw e;
        }

        //4: put the started pool to concurrent map(global)
        categoryPoolMap.put(key, categoryPool);

        //5: return the started pool to get objects
        return categoryPool;
    }

    //***************************************************************************************************************//
    //                                     4: Pooled keys maintenance(8+0)                                           //
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
                this.collectPoolLogs);
        if (includeKeys) {
            for (ObjectKeyCategoryPool<K, V> pool : categoryPoolMap.values()) {
                ObjectKeyMonitorVo keyMonitorVo = pool.getKeyMonitorVo();
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
        final long parkTimeForRetryNs = defaultCategoryPool != null ? defaultCategoryPool.getParkTimeForRetryNs() : MILLISECONDS.toNanos(10L);

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
        for (ObjectKeyCategoryPool<K, V> categoryPool : this.categoryPoolMap.values())
            categoryPool.close(forceRecycleBorrowed);
        this.categoryPoolMap.clear();
        this.keyCounter = null;
        this.defaultKey = null;
        this.defaultCategoryPool = null;

        //5: Unregister MBeans
        if (isNotBlank(this.poolNameOfRegisteredMBean))
            this.unregisterMBeans();

        //6: unregister Pool hook
        if (this.exitHook != null) {//this method call is from pool close
            try {//remove Hook
                Runtime.getRuntime().removeShutdownHook(this.exitHook);
            } catch (Throwable e) {
                //do nothing
            } finally {
                this.exitHook = null;
            }
        }
    }

    //***************************************************************************************************************//
    //                                    6: Pool blocking interrupts(2+0)                                           //
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
    //                                    7: method execution logs(4+0)                                              //
    //***************************************************************************************************************//
    public void enableLogCache(boolean enable) {
        if (this.collectPoolLogs != enable) {
            this.collectPoolLogs = enable;
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
    public ObjectPoolMonitorVo getPoolMonitorVo() throws Exception {
        return (ObjectPoolMonitorVo) this.getPoolMonitorVo(false);
    }

    public List<String> getKeyNames() throws Exception {
        List<String> keyNameList = new LinkedList<>();
        for (ObjectKeyCategoryPool<K, V> categoryPool : this.categoryPoolMap.values()) {
            keyNameList.add(categoryPool.getKeyName());
        }
        return keyNameList;
    }

    public void enableKeyLogPrinterByName(String keyName, boolean enable) throws Exception {
        for (ObjectKeyCategoryPool<K, V> categoryPool : this.categoryPoolMap.values()) {
            if (categoryPool.getKeyName().equals(keyName)) {
                categoryPool.enableLogPrint(enable);
                break;
            }
        }
    }

    public void enableKeyLogCacheByName(String keyName, boolean enable) throws Exception {
        for (ObjectKeyCategoryPool<K, V> categoryPool : this.categoryPoolMap.values()) {
            if (categoryPool.getKeyName().equals(keyName)) {
                categoryPool.enableLogCache(enable);
                break;
            }
        }
    }

    public ObjectKeyMonitorVo getKeyMonitorVoByName(String keyName) throws Exception {
        for (ObjectKeyCategoryPool<K, V> categoryPool : this.categoryPoolMap.values()) {
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
