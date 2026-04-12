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
import org.stone.beeop.exception.*;
import org.stone.tools.LogPrinter;
import org.stone.tools.extension.InterruptableSemaphore;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beeop.BeeMethodLog.*;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;
import static org.stone.tools.LogPrinter.getLogPrinter;

/**
 * A pool impl to maintain pooled objects
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledObjectBucket<K, V> extends PooledObjectBucketLogCache<K> implements Runnable, Cloneable {
    private static final VarHandle ObjStUpd;
    private static final VarHandle BorrowStUpd;
    private static final VarHandle PoolStateUpd;
    private static final VarHandle ServantStateUpd;
    private static final VarHandle ServantTryCountUpd;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            ObjStUpd = l.findVarHandle(PooledObject.class, "state", int.class);
            BorrowStUpd = l.findVarHandle(Borrower.class, "state", Object.class);
            PoolStateUpd = l.findVarHandle(PooledObjectBucket.class, "poolState", int.class);
            ServantStateUpd = l.findVarHandle(PooledObjectBucket.class, "servantState", int.class);
            ServantTryCountUpd = l.findVarHandle(PooledObjectBucket.class, "servantTryCount", int.class);
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    //Clone Start
    private final ObjectPool<K, V> parentPool;
    private final boolean isFairMode;
    private final boolean isCompeteMode;
    private final int maxActiveSize;
    private final int semaphoreSize;
    private final long maxWaitMs;//milliseconds
    private final long maxWaitNs;//nanoseconds

    private final long idleTimeoutMs;//milliseconds
    private final long holdTimeoutMs;//milliseconds
    private final boolean supportHoldTimeout;
    private final int stateCodeOnRelease;
    private final long validAssumeTime;//milliseconds
    private final int validTestTimeout;//seconds
    private final long parkTimeForRetryNs;//nanoseconds
    private final boolean useThreadLocal;
    private final long methodLogsTimeoutMs;
    private final long intervalOfMethodLogsClearTaskMs;
    private final long intervalOfObjectsClearTask;

    private final String[] configuredMethodNames;
    private final boolean hasConfiguredMethodNames;

    private final BeeObjectPredicate objectPredicate;
    private final BeeObjectFactory<K, V> objectFactory;//create objects to be pooled
    private final ObjectTransferPolicy<K, V> transferPolicy;//transfer objects to waiters
    private final Map<MethodKey, MethodHandle> objectMethodCacheMap;//cache called methods
    private final ObjectPlainHandleFactory<K, V> handleFactory;//create object handle to borrowers
    private final ScheduledThreadPoolExecutor scheduledService;
    private final int methodLogCacheSize;
    private final BeeMethodLogListener<K> methodLogListener;//changeable
    private final long getSlowThreshold;
    private final long callSlowThreshold;
    LogPrinter logPrinter = DefaultLogPrinter;
    boolean collectMethodLogs;//changeable
    //Clone end
    //category key
    private K key;
    //key name
    private String keyName;
    //Category pool state
    private volatile int poolState;
    //State of category servant
    private volatile int servantState;
    //Retry count to get idle object
    private volatile int servantTryCount;
    //Semaphore of category pool
    private InterruptableSemaphore semaphore;
    //Array store pooled objects
    private PooledObject<K, V>[] objectArray;
    //Wait queue
    private ConcurrentLinkedQueue<Borrower<K, V>> waitQueue;
    //ThreadLocal caches last used pooled objects for borrowers
    private ThreadLocal<WeakReference<Borrower<K, V>>> threadLocal;

    //Handle of scheduled task clear timeout logs
    private ScheduledFuture<?> timeoutLogsClearTaskFuture;
    //Handle of scheduled task clear timeout objects
    private ScheduledFuture<?> timeoutObjectsClearTaskFuture;

    //***************************************************************************************************************//
    //                                     1: Pool Creation(1+1)                                                     //
    //***************************************************************************************************************//
    PooledObjectBucket(ObjectPool<K, V> parentPool, BeeObjectSourceConfig<K, V> config,
                       Constructor<?> objectProxyClassConstructor, ScheduledThreadPoolExecutor scheduledService) {

        //step1: copy  primitive type field
        this.parentPool = parentPool;
        this.poolState = POOL_NEW;

        this.useThreadLocal = config.isUseThreadLocal();
        this.semaphoreSize = config.getSemaphoreSize();
        this.maxActiveSize = config.getMaxActive();

        this.maxWaitMs = config.getMaxWait();
        this.maxWaitNs = MILLISECONDS.toNanos(maxWaitMs);//nanoseconds
        this.idleTimeoutMs = config.getIdleTimeout();
        this.holdTimeoutMs = config.getHoldTimeout();

        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.parkTimeForRetryNs = MILLISECONDS.toNanos(config.getParkTimeForRetry());
        this.validAssumeTime = config.getAliveAssumeTime();
        this.validTestTimeout = config.getAliveTestTimeout();
        this.methodLogsTimeoutMs = config.getLogTimeout();
        this.intervalOfMethodLogsClearTaskMs = config.getIntervalOfClearTimeoutLogs();
        this.intervalOfObjectsClearTask = config.getIntervalOfClearTimeout();

        //step2:object type field setting
        this.objectPredicate = config.getPredicate();
        this.objectFactory = config.getObjectFactory();
        List<String> methodNameList = config.getObjectMethodNameList();
        this.configuredMethodNames = methodNameList == null ? null : methodNameList.toArray(new String[0]);
        this.hasConfiguredMethodNames = configuredMethodNames != null && configuredMethodNames.length > 0;

        this.objectMethodCacheMap = new ConcurrentHashMap<>(1);
        this.scheduledService = scheduledService;
        this.collectMethodLogs = config.isEnableLogCache();
        this.methodLogCacheSize = config.getLogCacheSize();
        this.methodLogListener = config.getLogListener();
        this.getSlowThreshold = config.getSlowGetThreshold();
        this.callSlowThreshold = config.getSlowCallThreshold();

        this.isFairMode = config.isFairMode();
        this.isCompeteMode = !isFairMode;
        this.transferPolicy = isFairMode ? new FairTransferPolicy<>() : new CompeteTransferPolicy<>();

        this.stateCodeOnRelease = transferPolicy.getStateCodeOnRelease();
        BeeObjectPredicate predicate = config.getPredicate();
        if (objectProxyClassConstructor != null)
            this.handleFactory = new ObjectProxyHandleFactory<>(predicate, objectProxyClassConstructor);
        else
            this.handleFactory = new ObjectPlainHandleFactory<>(predicate);
    }

    @SuppressWarnings("unchecked")
    PooledObjectBucket<K, V> createByClone() throws Exception {
        return (PooledObjectBucket<K, V>) clone();
    }

    //***************************************************************************************************************//
    //                                     2: Pool start(0+1)                                                        //
    //***************************************************************************************************************//
    @SuppressWarnings("uncheck")
    void startup(K key, int initSize, boolean asyncCreateInitObjects, boolean isPrintRuntimeLogs) throws Exception {
        try {
            //step1: set key and log printer
            this.key = key;
            this.keyName = key.toString();
            this.logPrinter = getLogPrinter(PooledObjectBucket.class, isPrintRuntimeLogs);

            //step2: Create array of pool objects and fill 'empty state' objects
            this.objectArray = new PooledObject[maxActiveSize];
            for (int i = 0; i < maxActiveSize; i++)
                objectArray[i] = new PooledObject<>(key,
                        this,
                        this.objectFactory,
                        this.objectPredicate,
                        this.hasConfiguredMethodNames,
                        this.configuredMethodNames,
                        this.objectMethodCacheMap);

            //step3: Schedule task to clear timeout objects(this task can interrupt blocking of objects creation)
            this.timeoutObjectsClearTaskFuture = this.scheduledService.scheduleWithFixedDelay(new TimeoutObjectsClearTask<>(this),
                    intervalOfObjectsClearTask, intervalOfObjectsClearTask, MILLISECONDS);

            //step4: Create initial objects
            if (initSize > 0 && !asyncCreateInitObjects) this.createInitObjects(initSize, true);

            //step5: Creates pool some internal objects
            if (this.useThreadLocal) this.threadLocal = new BorrowerThreadLocal<>();
            this.semaphore = new InterruptableSemaphore(semaphoreSize, isFairMode);
            this.waitQueue = new ConcurrentLinkedQueue<>();
            this.servantTryCount = 0;
            this.servantState = THREAD_WAITING;//initial state

            //Step6: Create initial objects by async mode(startup a new thread to create initial objects)
            if (initSize > 0 && asyncCreateInitObjects) new PoolInitAsyncCreateThread<>(initSize, this).start();

            //step7: log cache initialize
            super.init(keyName, this.methodLogCacheSize, this.methodLogListener);
            super.setSlowThreshold(Type_Key_Log, getSlowThreshold);
            super.setSlowThreshold(Type_Object_Log, callSlowThreshold);
            if (this.collectMethodLogs) {
                this.timeoutLogsClearTaskFuture = this.scheduledService.scheduleWithFixedDelay(new TimeoutMethodLogsClearTask<>(this, methodLogsTimeoutMs),
                        intervalOfMethodLogsClearTaskMs, intervalOfMethodLogsClearTaskMs, MILLISECONDS);
            }

            //step8: print completion message and set pool to ready state
            String poolMode = this.isFairMode ? "fair" : "compete";
            logPrinter.info("BeeOP({})-has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms",
                    this.keyName,
                    poolMode,
                    initSize,
                    this.maxActiveSize,
                    this.semaphoreSize,
                    this.maxWaitMs);

            this.poolState = POOL_READY;
        } catch (Throwable e) {
            if (timeoutObjectsClearTaskFuture != null) {
                timeoutObjectsClearTaskFuture.cancel(true);
                timeoutObjectsClearTaskFuture = null;
            }

            if (timeoutLogsClearTaskFuture != null) {
                timeoutLogsClearTaskFuture.cancel(true);
                timeoutLogsClearTaskFuture = null;
            }
            throw e;
        }
    }

    //***************************************************************************************************************//
    //                                         3: Pooled Objects Creation(0+2)                                       //
    //***************************************************************************************************************//
    private void createInitObjects(int initSize, boolean syn) throws Exception {
        int index = 0;
        try {
            Thread creatingThread = Thread.currentThread();
            while (index < initSize) {
                PooledObject<K, V> p = objectArray[index++];
                p.state = OBJECT_CREATING;
                this.fillObjectInstance(p, OBJECT_IDLE, creatingThread);
            }
        } catch (Throwable e) {
            if (syn) {
                for (int i = 0; i < index; i++)
                    objectArray[i].onRemove(DESC_RM_POOL_INIT);
                throw e;
            } else {
                logPrinter.warn("Failed to create initial objects during async mode", e);
            }
        }
    }

    private PooledObject<K, V> fillObjectInstance(PooledObject<K, V> p, int state, Thread creatingThread) throws Exception {
        //1: print runtime log of object creation
        logPrinter.info("BeeOP({})-begin to create a raw object", this.keyName);

        V instance = null;
        try {
            p.creatingInfo = new ObjectCreatingInfo(creatingThread);
            instance = this.objectFactory.create(this.key);
            if (instance == null) {//if blocking interrupt on LockSupport.park in factory,maybe just return a null object?
                if (creatingThread.isInterrupted() && Thread.interrupted())
                    throw new BeePooledObjectGetInterruptedException("An interruption occurred during creating object instance");
                throw new BeePooledObjectCreationException("Object instance created failed,null result returned from object factory");
            }

            objectFactory.setDefault(key, instance);//set default on created instance
            p.setObjectInstance(state, instance);//fill the created instance to pooled wrapper

            logPrinter.info("BeeOP({})-created a new object instance:{} to fill pooled wrapper:{}", this.keyName, instance, p);
            return p;
        } catch (Throwable e) {
            p.state = OBJECT_CLOSED;//reset to closed state
            if (instance != null) this.objectFactory.destroy(key, instance);
            throw new BeePooledObjectCreationException(e);
        } finally {
            p.creatingInfo = null;
        }
    }

    //***************************************************************************************************************//
    //                                         4: Pooled objects get(1+4)                                            //                                                                                  //
    //***************************************************************************************************************//
    public BeeObjectHandle<K, V> getObjectHandle(long startTime) throws Exception {
        if (this.collectMethodLogs) {
            BeeMethodLog<K> log = this.beforeCall(startTime, this.key, Type_Key_Log, "ObjectKeyCategoryPool.getObjectHandle()", new Object[]{startTime});
            try {
                BeeObjectHandle<K, V> handle = this.getObjectHandleInternal(startTime);
                this.afterCall(System.currentTimeMillis(), handle, log);
                return handle;
            } catch (Throwable e) {
                this.afterCall(System.currentTimeMillis(), e, log);
                throw e;
            }
        } else {
            return this.getObjectHandleInternal(startTime);
        }
    }

    //*** Core method for get *****
    private BeeObjectHandle<K, V> getObjectHandleInternal(long startTime) throws Exception {
        if (this.poolState != POOL_READY)
            throw new BeePooledObjectKeyException("Key was not ready");

        //1: try to reuse object in thread local
        Borrower<K, V> b = null;
        PooledObject<K, V> p;
        if (this.useThreadLocal) {
            b = this.threadLocal.get().get();
            if (b != null) {
                p = b.lastUsed;
                if (p != null) {
                    int state = p.state;
                    if (state == OBJECT_IDLE) {
                        if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_BORROWED)) {
                            if (this.testOnBorrow(p)) return handleFactory.createHandle(p);
                        } else if (p.state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                            return handleFactory.createHandle(this.fillObjectInstance(p, OBJECT_BORROWED, b.thread));
                        }
                    } else if (state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                        return handleFactory.createHandle(this.fillObjectInstance(p, OBJECT_BORROWED, b.thread));
                    }
                }
            }
        }

        try {
            //2: try to acquire a permit from pool semaphore
            long deadline = startTime > 0L ? startTime : System.currentTimeMillis();
            if (this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS)) {
                try {
                    //3: try to search idle one or create new one
                    Thread borrowThread = b != null ? b.thread : Thread.currentThread();
                    p = this.searchOrCreate(borrowThread);
                    if (p != null) {
                        if (this.useThreadLocal) {
                            if (b != null)
                                b.lastUsed = p;
                            else
                                this.threadLocal.set(new WeakReference<>(new Borrower<>(borrowThread, p)));
                        }
                        return handleFactory.createHandle(p);
                    }

                    //4: add the borrower to wait queue
                    if (b != null) {
                        b.state = null;
                    } else {
                        b = new Borrower<>(borrowThread);
                        if (this.useThreadLocal) this.threadLocal.set(new WeakReference<>(b));
                    }

                    this.waitQueue.offer(b);
                    deadline += this.maxWaitMs;

                    //5: self-spin to get transferred object
                    do {
                        final Object s = b.state;//possible values: PooledObject,Throwable,null
                        if (s instanceof PooledObject) {
                            p = (PooledObject) s;
                            if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                                this.waitQueue.remove(b);
                                b.lastUsed = p;
                                return handleFactory.createHandle(p);
                            }
                        } else if (s instanceof Throwable) {//here: s must be throwable object
                            this.waitQueue.remove(b);
                            throw s instanceof Exception ? (Exception) s : new BeePooledObjectGetException((Throwable) s);
                        }

                        long t = deadline - System.currentTimeMillis();
                        if (t > 0L) {
                            if (s != null) b.state = null;
                            if (this.servantTryCount > 0 && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
                                parentPool.submitServantTask(this);
                            LockSupport.parkNanos(MILLISECONDS.toNanos(t));//park exit:1:get transfer 2:timeout 3:interrupted
                            if (borrowThread.isInterrupted() && Thread.interrupted())
                                this.handleTimeoutAndInterruption(false, null, b);
                        } else {//timeout
                            return this.handleTimeoutAndInterruption(true, s, b);
                        }
                    } while (true);//while
                } finally {
                    semaphore.release();
                }
            } else {
                throw new BeePooledObjectGetTimeoutException("Waited timeout on pool semaphore");
            }
        } catch (InterruptedException e) {
            throw new BeePooledObjectGetInterruptedException("An interruption occurred while waiting on pool semaphore");
        }
    }

    /*** Search or create connection ***/
    private PooledObject<K, V> searchOrCreate(Thread creatingThread) throws Exception {
        for (PooledObject<K, V> p : objectArray) {
            int state = p.state;
            if (state == OBJECT_IDLE) {
                if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_BORROWED)) {
                    if (this.testOnBorrow(p)) return p;
                } else if (p.state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                    return this.fillObjectInstance(p, OBJECT_BORROWED, creatingThread);
                }
            } else if (state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                return this.fillObjectInstance(p, OBJECT_BORROWED, creatingThread);
            }
        }
        return null;
    }

    /*** alive test on borrowed connection ***/
    private boolean testOnBorrow(PooledObject<K, V> p) {
        try {
            if (System.currentTimeMillis() - p.lastAccessTime - this.validAssumeTime >= 0L && !this.objectFactory.isValid(key, p.objectInstance, this.validTestTimeout)) {
                p.onRemove(DESC_RM_BAD);
                this.tryWakeupServantThread();
                return false;
            } else {
                return true;
            }
        } catch (Throwable e) {
            logPrinter.warn("BeeOP({})-alive test failed on a borrowed object", this.keyName, e);
        }
        return false;
    }

    /*** alive test on borrowed connection ***/
    private BeeObjectHandle<K, V> handleTimeoutAndInterruption(boolean isTimeout, Object s, Borrower<K, V> b) throws Exception {
        this.waitQueue.remove(b);

        PooledObject<K, V> p = null;
        if (s == null) {
            s = b.state;
            if (s instanceof PooledObject) {
                p = (PooledObject) s;
                if (!(this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)))
                    p = null;
            } else if (s instanceof Throwable) {
                throw s instanceof Exception ? (Exception) s : new BeePooledObjectGetException((Throwable) s);
            }
        }

        if (isTimeout) {
            if (p != null) {
                b.lastUsed = p;
                return handleFactory.createHandle(p);
            }
            throw new BeePooledObjectGetTimeoutException("Waited timeout for a released object");
        } else {
            if (p != null) this.recycle(p);
            throw new BeePooledObjectGetInterruptedException("An interruption occurred while waiting for a released object");
        }
    }

    //***************************************************************************************************************//
    //                                         5: Pooled Objects recycle(0+4)                                        //
    //***************************************************************************************************************//
    void recycle(PooledObject<K, V> p) {
        if (isCompeteMode) p.state = OBJECT_IDLE;

        for (Borrower<K, V> b : waitQueue) {
            if (p.state != stateCodeOnRelease) return;
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, p)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }

        if (isFairMode) p.state = OBJECT_IDLE;
        tryWakeupServantThread();
    }

    private void transferException(Throwable e) {
        for (Borrower<K, V> b : waitQueue) {
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    void abort(PooledObject<K, V> p, String reason) {
        p.onRemove(reason);
        this.tryWakeupServantThread();
    }

    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount;
            if (c >= this.maxActiveSize) return;
        } while (!ServantTryCountUpd.compareAndSet(this, c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING)) {
            parentPool.submitServantTask(this);
        }
    }

    //***************************************************************************************************************//
    //                                         6: Pool restart(0+2)                                                  //                                                                                  //
    //***************************************************************************************************************//
    boolean restart(boolean forceRecycleBorrowed) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_RESTARTING)) {
            logPrinter.info("BeeOP({})-begin to clear all objects", this.keyName);
            this.removeAllObjects(forceRecycleBorrowed, DESC_RM_POOL_CLEAR);
            logPrinter.info("BeeOP({})-has clear all objects", this.keyName);
            this.poolState = POOL_READY;// restore state;
            logPrinter.info("BeeOP({})-pool has cleared all objects", this.keyName);
            return true;
        } else {
            return false;
        }
    }

    private void removeAllObjects(boolean forceRecycleBorrowed, String removeReason) {
        //1:interrupt all waiting thread
        this.interruptWaitingThreads();

        //2:clear all connections
        int closedCount = 0;
        while (true) {
            for (PooledObject<K, V> p : this.objectArray) {
                final int state = p.state;
                if (state == OBJECT_IDLE) {
                    if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_CLOSED)) {
                        closedCount++;
                        p.onRemove(removeReason);
                    }
                } else if (state == OBJECT_BORROWED) {
                    BeeObjectHandle<K, V> handleInUsing = p.handleInUsing;
                    if (handleInUsing != null) {
                        if (forceRecycleBorrowed || (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L))
                            tryCloseObjectHandle(handleInUsing);
                    }
                } else if (state == OBJECT_CLOSED) {
                    closedCount++;
                }
            }

            if (closedCount == this.maxActiveSize) break;
            LockSupport.parkNanos(this.parkTimeForRetryNs);
            closedCount = 0;
        } // while

        if (logPrinter.isEnableLogOutput()) {
            BeeObjectKeyMonitorVo vo = this.getKeyMonitorVo();
            logPrinter.info("BeeOP({})-idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.keyName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                         6: Pool Suspend (2+0)                                                 //
    //***************************************************************************************************************//
    public boolean suspendKey() {
        return PoolStateUpd.compareAndSet(this, POOL_READY, POOL_SUSPENDED);
    }

    public boolean resumeKey() {
        return PoolStateUpd.compareAndSet(this, POOL_SUSPENDED, POOL_READY);
    }

    //***************************************************************************************************************//
    //                                         7: Pool Close(2+0)                                                    //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    public void close(boolean forceRecycleBorrowed) {
        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_NEW && PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_CLOSED)) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_RESTARTING) {
                LockSupport.parkNanos(this.parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                logPrinter.info("BeeOP({})-begin to shutdown", this.keyName);
                this.removeAllObjects(forceRecycleBorrowed, DESC_RM_POOL_SHUTDOWN);

                if (timeoutObjectsClearTaskFuture != null) {
                    timeoutObjectsClearTaskFuture.cancel(true);
                    timeoutObjectsClearTaskFuture = null;
                }

                if (timeoutLogsClearTaskFuture != null) {
                    timeoutLogsClearTaskFuture.cancel(true);
                    timeoutLogsClearTaskFuture = null;
                }

                this.poolState = POOL_CLOSED;
                logPrinter.info("BeeOP({})-has shutdown", this.keyName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }


    //***************************************************************************************************************//
    //                                         8: Key method logs (2+0)                                              //
    //***************************************************************************************************************//
    public synchronized void enableLogCache(boolean enable) {
        if (this.collectMethodLogs != enable) {
            this.collectMethodLogs = enable;
            if (enable) {
                this.timeoutLogsClearTaskFuture = this.scheduledService.scheduleWithFixedDelay(new TimeoutMethodLogsClearTask<>(this, methodLogsTimeoutMs),
                        intervalOfMethodLogsClearTaskMs, intervalOfMethodLogsClearTaskMs, MILLISECONDS);
            } else if (this.timeoutLogsClearTaskFuture != null) {
                this.clearLogs(Type_All);
                this.timeoutLogsClearTaskFuture.cancel(true);
                this.timeoutLogsClearTaskFuture = null;
            }
        }
    }

    //***************************************************************************************************************//
    //                                         9: other methods (2+2)                                                //
    //***************************************************************************************************************//
    String getKeyName() {
        return keyName;
    }

    long getParkTimeForRetryNs() {
        return this.parkTimeForRetryNs;
    }

    public synchronized void enableLogPrint(boolean enable) {
        this.logPrinter = LogPrinter.getLogPrinter(PooledObjectBucket.class, enable);
    }

    public List<Thread> interruptWaitingThreads() {
        //1clear waiting thread on semaphore
        List<Thread> threads = new LinkedList<>(this.semaphore.interruptQueuedWaitThreads());

        //2: transfer exception to waiter in queue
        if (!this.waitQueue.isEmpty()) {
            BeeObjectSourcePoolRestartedFailureException exception = new BeeObjectSourcePoolRestartedFailureException("Pool has been closed or is restarting");
            while (!this.waitQueue.isEmpty()) this.transferException(exception);
        }

        //3: attempt to interrupt creating of connections
        for (PooledObject<K, V> p : objectArray) {
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) {
                creatingInfo.creatingThread.interrupt();
                threads.add(creatingInfo.creatingThread);
            }
        }

        return threads;
    }

    public PooledObjectBucketMonitorVo getKeyMonitorVo() {
        int borrowedSize = 0, idleSize = 0;
        int creatingCount = 0, creatingTimeoutCount = 0;

        if (objectArray != null) {
            long currentTimeMillis = System.currentTimeMillis();
            for (PooledObject<K, V> p : objectArray) {
                if (p != null) {
                    int state = p.state;
                    if (state == OBJECT_IDLE) idleSize++;
                    if (state == OBJECT_BORROWED) borrowedSize++;

                    ObjectCreatingInfo creatingInfo = p.creatingInfo;
                    if (creatingInfo != null) {
                        creatingCount++;
                        if (currentTimeMillis - creatingInfo.creatingStartTime - maxWaitMs >= 0L)
                            creatingTimeoutCount++;
                    }
                }
            }
        }

        int semaphoreRemainSize = 0;
        int semaphoreWaitingSize = 0;
        int transferWaitingSize = 0;
        if (semaphore != null) {
            semaphoreRemainSize = this.semaphore.availablePermits();
            semaphoreWaitingSize = this.semaphore.getQueueLength();
        }

        if (waitQueue != null) {
            for (Borrower<K, V> borrower : this.waitQueue)
                if (borrower.state == null) transferWaitingSize++;
        }

        return new PooledObjectBucketMonitorVo(this.keyName, poolState,
                idleSize, borrowedSize, creatingCount, creatingTimeoutCount,
                semaphoreRemainSize, semaphoreWaitingSize, transferWaitingSize,
                this.logPrinter.isEnableLogOutput(), this.collectMethodLogs);
    }

    //***************************************************************************************************************//
    //                                         10: close objects(0+2)[Timer task call]                               //
    //***************************************************************************************************************//
    void clearIdleTimeoutObjects() {
        //step1: print pool info before clean
        if (logPrinter.isEnableLogOutput()) {
            BeeObjectKeyMonitorVo vo = this.getKeyMonitorVo();
            logPrinter.info("BeeOP({})-before idle clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.keyName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2: attempt to interrupt timeout creation
        this.interruptObjectCreating();

        //step3: remove idle timeout and hold timeout
        for (PooledObject<K, V> p : this.objectArray) {
            int state = p.state;
            if (state == OBJECT_IDLE && this.semaphore.availablePermits() == this.semaphoreSize) {//no borrowers on semaphore
                boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime - this.idleTimeoutMs >= 0L;
                if (isTimeoutInIdle && ObjStUpd.compareAndSet(p, state, OBJECT_CLOSED)) {//need close idle
                    p.onRemove(DESC_RM_IDLE);
                    this.tryWakeupServantThread();
                }
            } else if (state == OBJECT_BORROWED && supportHoldTimeout) {
                if (System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L) {//hold timeout
                    BeeObjectHandle<K, V> handleInUsing = p.handleInUsing;
                    if (handleInUsing != null) tryCloseObjectHandle(handleInUsing);
                }
            }
        }

        //step4: print pool info after idle clean
        if (logPrinter.isEnableLogOutput()) {
            BeeObjectKeyMonitorVo vo = this.getKeyMonitorVo();
            logPrinter.info("BeeOP({})-after idle clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.keyName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    private void interruptObjectCreating() {
        for (PooledObject<K, V> p : objectArray) {
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null && System.currentTimeMillis() - creatingInfo.creatingStartTime - maxWaitMs >= 0L) {
                creatingInfo.creatingThread.interrupt();
            }
        }
    }

    //***************************************************************************************************************//
    //                                         11: Servant task method(1)                                            //                                                                              //
    //***************************************************************************************************************//
    public void run() {
        Thread currentThread = Thread.currentThread();

        while (servantTryCount > 0 && !waitQueue.isEmpty()) {
            if (ServantTryCountUpd.compareAndSet(this, servantTryCount, servantTryCount - 1)) {
                try {
                    PooledObject<K, V> p = searchOrCreate(currentThread);
                    if (p != null) recycle(p);
                } catch (Throwable e) {
                    this.transferException(e);
                }
            }
        }

        this.servantState = THREAD_WAITING;
    }

    //***************************************************************************************************************//
    //                                         12: Pool Internal classes(0+6)                                        //                                                                              //
    //***************************************************************************************************************//
    private static class ObjectPlainHandleFactory<K, V> {
        protected final BeeObjectPredicate predicate;

        ObjectPlainHandleFactory(BeeObjectPredicate predicate) {
            this.predicate = predicate;
        }

        BeeObjectHandle<K, V> createHandle(PooledObject<K, V> p) throws Exception {
            return new ObjectHandleImpl<>(p);
        }
    }

    private static class ObjectProxyHandleFactory<K, V> extends ObjectPlainHandleFactory<K, V> {
        private final Constructor<?> objectProxyClassConstructor;

        ObjectProxyHandleFactory(BeeObjectPredicate predicate,
                                 Constructor<?> objectProxyClassConstructor) {
            super(predicate);
            this.objectProxyClassConstructor = objectProxyClassConstructor;
        }

        BeeObjectHandle<K, V> createHandle(PooledObject<K, V> p) throws Exception {
            return new ObjectHandleImpl.ObjectHandleImpl2<>(p, predicate, objectProxyClassConstructor);
        }
    }

    private static final class BorrowerThreadLocal<K, V> extends ThreadLocal<WeakReference<Borrower<K, V>>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<Borrower<K, V>> initialValue() {
            return new WeakReference<>(new Borrower<>(Thread.currentThread()));
        }
    }

    private static final class FairTransferPolicy<K, V> implements ObjectTransferPolicy<K, V> {
        public int getStateCodeOnRelease() {
            return OBJECT_BORROWED;
        }

        public boolean tryCatch(PooledObject<K, V> p) {
            return p.state == OBJECT_BORROWED;
        }
    }

    private static final class CompeteTransferPolicy<K, V> implements ObjectTransferPolicy<K, V> {
        public int getStateCodeOnRelease() {
            return OBJECT_IDLE;
        }

        public boolean tryCatch(PooledObject<K, V> p) {
            return p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_BORROWED);
        }
    }

    private static final class PoolInitAsyncCreateThread<K, V> extends Thread {
        private final int initialSize;
        private final PooledObjectBucket<K, V> pool;

        PoolInitAsyncCreateThread(int initialSize, PooledObjectBucket<K, V> pool) {
            this.initialSize = initialSize;
            this.pool = pool;
        }

        public void run() {
            try {
                pool.createInitObjects(initialSize, false);
                pool.servantTryCount = pool.objectArray.length;

                if (!pool.waitQueue.isEmpty() && pool.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(pool, THREAD_WAITING, THREAD_WORKING)) {
                    pool.parentPool.submitServantTask(pool);
                }
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    private record TimeoutObjectsClearTask<K, V>(PooledObjectBucket<K, V> categoryPool) implements Runnable {

        public void run() {
            try {
                this.categoryPool.clearIdleTimeoutObjects();
            } catch (Throwable e) {
                categoryPool.logPrinter.warn("BeeOP({})-an exception occurred while scanning timeout method logs", this.categoryPool.keyName, e);
            }
        }
    }

    private record TimeoutMethodLogsClearTask<K, V>(PooledObjectBucket<K, V> categoryPool,
                                                    long timeout) implements Runnable {

        public void run() {
            try {
                categoryPool.clearTimeoutLogs(timeout);
            } catch (Throwable e) {
                categoryPool.logPrinter.warn("BeeOP({})-an exception occurred while scanning timeout method logs", this.categoryPool.keyName, e);
            }
        }
    }
}
