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
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;
import org.stone.tools.extension.InterruptableSemaphore;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.LogPrinter.getLogPrinter;

/**
 * Category pool to maintain pooled objects by key.
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ObjectKeyCategoryPool<K, V> implements Runnable, Cloneable, ObjectKeyCategoryPoolMXBean {
    static final AtomicIntegerFieldUpdater<PooledObject> ObjStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledObject.class, "state");
    static final AtomicIntegerFieldUpdater<ObjectKeyCategoryPool> ServantStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectKeyCategoryPool.class, "servantState");
    private static final AtomicIntegerFieldUpdater<ObjectKeyCategoryPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectKeyCategoryPool.class, "poolState");
    private static final AtomicIntegerFieldUpdater<ObjectKeyCategoryPool> ServantTryCountUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectKeyCategoryPool.class, "servantTryCount");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    final KeyedObjectPool<K, V> parentPool;

    //clone begin
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
    private final BeeObjectFactory<K, V> objectFactory;//create objects to be pooled
    private final ObjectPlainHandleFactory<K, V> handleFactory;//create object handle to borrowers
    private final ObjectTransferPolicy<K, V> transferPolicy;//transfer objects to waiters
    private final Map<MethodCacheKey, Method> methodCacheMap;//cache called methods
    LogPrinter logPrinter;
    //clone end

    //category key
    private K key;
    //parent's name + key.string()
    private String poolName;
    //category pool state
    private volatile int poolState;
    //State of category servant
    private volatile int servantState;
    //represents retry count to get pooled objects
    private volatile int servantTryCount;

    //category pool semaphore
    private InterruptableSemaphore semaphore;
    //fixed length array to store pooled objects
    private PooledObject<K, V>[] objectArray;
    //Wait queue
    private ConcurrentLinkedQueue<Borrower<K, V>> waitQueue;
    //thread local to cache last borrowed objects for borrowers
    private ThreadLocal<WeakReference<Borrower<K, V>>> threadLocal;

    //***************************************************************************************************************//
    //                                         1: Pool Creation/Start(1+2)                                           //
    //***************************************************************************************************************//
    ObjectKeyCategoryPool(KeyedObjectPool<K, V> parentPool, BeeObjectSourceConfig<K, V> config,
                          Constructor<?> objectProxyClassConstructor) {
        //step1: copy  primitive type field
        this.parentPool = parentPool;
        this.poolState = POOL_NEW;

        this.useThreadLocal = config.isUseThreadLocal();
        this.semaphoreSize = config.getSemaphoreSize();
        this.maxActiveSize = config.getMaxActive();

        this.maxWaitMs = config.getMaxWait();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(maxWaitMs);//nanoseconds
        this.idleTimeoutMs = config.getIdleTimeout();
        this.holdTimeoutMs = config.getHoldTimeout();

        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.parkTimeForRetryNs = TimeUnit.MILLISECONDS.toNanos(config.getParkTimeForRetry());
        this.validAssumeTime = config.getAliveAssumeTime();
        this.validTestTimeout = config.getAliveTestTimeout();

        //step2:object type field setting
        this.objectFactory = config.getObjectFactory();
        this.methodCacheMap = new ConcurrentHashMap<>(1);

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
    ObjectKeyCategoryPool<K, V> createByClone() throws Exception {
        return (ObjectKeyCategoryPool<K, V>) clone();
    }

    void startup(String parentName, K key, int initSize, boolean asyncCreateInitObjects, boolean isPrintRuntimeLogs) throws Exception {
        this.key = key;
        this.poolName = parentName + "-[" + key + "]";
        this.logPrinter = getLogPrinter(ObjectKeyCategoryPool.class, isPrintRuntimeLogs);

        this.objectArray = new PooledObject[maxActiveSize];
        for (int i = 0; i < maxActiveSize; i++)
            objectArray[i] = new PooledObject(key, objectFactory, methodCacheMap, this);

        if (initSize > 0 && !asyncCreateInitObjects) this.createInitObjects(initSize, true);
        if (this.useThreadLocal) this.threadLocal = new BorrowerThreadLocal<>();
        this.semaphore = new InterruptableSemaphore(semaphoreSize, isFairMode);
        this.waitQueue = new ConcurrentLinkedQueue<>();

        this.servantTryCount = 0;
        this.servantState = THREAD_WAITING;//initial state

        if (initSize > 0 && asyncCreateInitObjects) new PoolInitAsyncCreateThread<>(initSize, this).start();
        String poolMode = this.isFairMode ? "fair" : "compete";
        this.poolState = POOL_READY;
        logPrinter.info("BeeOP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms",
                this.poolName,
                poolMode,
                initSize,
                this.maxActiveSize,
                this.semaphoreSize,
                this.maxWaitMs);
    }

    //***************************************************************************************************************//
    //                                         2: Pooled Objects Creation(0+2)                                       //
    //***************************************************************************************************************//
    private void createInitObjects(int initSize, boolean syn) throws Exception {
        int index = 0;
        try {
            Thread creatingThread = Thread.currentThread();
            while (index < initSize) {
                PooledObject<K, V> p = objectArray[index++];
                p.state = OBJECT_CREATING;
                this.fillRawObject(p, OBJECT_IDLE, creatingThread);
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

    private PooledObject<K, V> fillRawObject(PooledObject<K, V> p, int state, Thread creatingThread) throws Exception {
        //1: print runtime log of object creation
        logPrinter.info("BeeOP({}))begin to create a raw object", this.poolName);

        V rawObj = null;
        try {
            p.creatingInfo = new ObjectCreatingInfo(creatingThread);
            rawObj = this.objectFactory.create(this.key);
            if (rawObj == null) {//if blocking interrupt on LockSupport.park in factory,maybe just return a null object?
                if (creatingThread.isInterrupted() && Thread.interrupted())
                    throw new ObjectGetInterruptedException("Interrupted on creating a raw object by factory");
                throw new ObjectCreatedException("Internal error occurred in object factory");
            }

            objectFactory.setDefault(key, rawObj);
            p.setRawObject(state, rawObj);

            logPrinter.info("BeeOP({})has created a new pooled object:{} with state:{}", this.poolName, p, state);

            return p;
        } catch (Throwable e) {
            p.state = OBJECT_CLOSED;//reset to closed state
            if (rawObj != null) this.objectFactory.destroy(key, rawObj);
            throw new ObjectCreatedException(e);
        } finally {
            p.creatingInfo = null;
        }
    }

    //***************************************************************************************************************//
    //                                         3: Pooled objects get(1+3)                                            //                                                                                  //
    //***************************************************************************************************************//
    //*** Core method for get *****
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new BeeObjectSourcePoolRejectedException("Pool has been closed or in clearing");

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
                            return handleFactory.createHandle(this.fillRawObject(p, OBJECT_BORROWED, b.thread));
                        }
                    } else if (state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                        return handleFactory.createHandle(this.fillRawObject(p, OBJECT_BORROWED, b.thread));
                    }
                }
            }
        }

        try {
            //2: try to acquire a permit from pool semaphore
            long deadline = System.currentTimeMillis();
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
                            throw s instanceof Exception ? (Exception) s : new ObjectGetException((Throwable) s);
                        }

                        long t = deadline - System.currentTimeMillis();
                        if (t > 0L) {
                            if (s != null) b.state = null;
                            if (this.servantTryCount > 0 && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
                                parentPool.submitServantTask(this);
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(t));//park exit:1:get transfer 2:timeout 3:interrupted
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
                throw new ObjectGetTimeoutException("Waited timeout on pool semaphore");
            }
        } catch (InterruptedException e) {
            throw new ObjectGetInterruptedException("An interruption occurred while waiting on pool semaphore");
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
                    return this.fillRawObject(p, OBJECT_BORROWED, creatingThread);
                }
            } else if (state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                return this.fillRawObject(p, OBJECT_BORROWED, creatingThread);
            }
        }
        return null;
    }

    /*** alive test on borrowed connection ***/
    private boolean testOnBorrow(PooledObject<K, V> p) {
        try {
            if (System.currentTimeMillis() - p.lastAccessTime - this.validAssumeTime >= 0L && !this.objectFactory.isValid(key, p.raw, this.validTestTimeout)) {
                p.onRemove(DESC_RM_BAD);
                this.tryWakeupServantThread();
                return false;
            } else {
                return true;
            }
        } catch (Throwable e) {
            logPrinter.warn("BeeOP({})alive test failed on a borrowed object", this.poolName, e);
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
                throw s instanceof Exception ? (Exception) s : new ObjectGetException((Throwable) s);
            }
        }

        if (isTimeout) {
            if (p != null) {
                b.lastUsed = p;
                return handleFactory.createHandle(p);
            }
            throw new ObjectGetTimeoutException("Waited timeout for a released object");
        } else {
            if (p != null) this.recycle(p);
            throw new ObjectGetInterruptedException("An interruption occurred while waiting for a released object");
        }
    }

    //***************************************************************************************************************//
    //                                         4: Pooled Objects recycle(0+4)                                        //
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
    //                                         5: Pool restart(0+2)                                                  //                                                                                  //
    //***************************************************************************************************************//
    boolean restart(boolean forceRecycleBorrowed) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_RESTARTING)) {
            logPrinter.info("BeeOP({})begin to clear all objects", this.poolName);
            this.removeAllObjects(forceRecycleBorrowed, DESC_RM_POOL_CLEAR);
            logPrinter.info("BeeOP({})has clear all objects", this.poolName);
            this.poolState = POOL_READY;// restore state;
            logPrinter.info("BeeOP({})pool has cleared all objects", this.poolName);
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
            BeeObjectKeyMonitorVo<K> vo = this.getKeyMonitorVo();
            logPrinter.info("BeeOP({})idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
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
                logPrinter.info("BeeOP({})begin to shutdown", this.poolName);
                this.removeAllObjects(forceRecycleBorrowed, DESC_RM_POOL_SHUTDOWN);

                this.poolState = POOL_CLOSED;
                logPrinter.info("BeeOP({})has shutdown", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                                         8: Pool method execution logs (5+1)                                   //
    //***************************************************************************************************************//


    //***************************************************************************************************************//
    //                                         9: MBean Registration (2+0)                                           //
    //***************************************************************************************************************//


    //***************************************************************************************************************//
    //                                         10: other methods (2+2)                                               //
    //***************************************************************************************************************//
    String getPoolName() {
        return poolName;
    }

    long getParkTimeForRetryNs() {
        return this.parkTimeForRetryNs;
    }

    public void enableLogPrint(boolean enable) {
        this.logPrinter = LogPrinter.getLogPrinter(ObjectKeyCategoryPool.class, enable);
    }

    public List<Thread> interruptWaitingThreads() {
        //1clear waiting thread on semaphore
        List<Thread> threads = new LinkedList<>(this.semaphore.interruptQueuedWaitThreads());

        //2: transfer exception to waiter in queue
        if (!this.waitQueue.isEmpty()) {
            BeeObjectSourcePoolRestartedException exception = new BeeObjectSourcePoolRestartedException("Pool has been closed or is restarting");
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

    public BeeObjectKeyMonitorVo<K> getKeyMonitorVo() {
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

        return new ObjectKeyMonitorVo<K>(this.key, poolState,
                idleSize, borrowedSize, creatingCount, creatingTimeoutCount,
                semaphoreRemainSize, semaphoreWaitingSize, transferWaitingSize,
                this.logPrinter.isEnableLogOutput());
    }

    //***************************************************************************************************************//
    //                                         11: close objects(0+2)[Timer task call]                               //
    //***************************************************************************************************************//
    void closeIdleTimeout() {
        //step1: print pool info before clean
        if (logPrinter.isEnableLogOutput()) {
            BeeObjectKeyMonitorVo<K> vo = this.getKeyMonitorVo();
            logPrinter.info("BeeOP({})-before idle clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
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
            BeeObjectKeyMonitorVo<K> vo = this.getKeyMonitorVo();
            logPrinter.info("BeeOP({})-after idle clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
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
    //                                         12: Servant task method(1)                                            //                                                                              //
    //***************************************************************************************************************//
    public void run() {
        Thread currentThread = Thread.currentThread();

        while (servantTryCount > 0 && !waitQueue.isEmpty()) {
            ServantTryCountUpd.decrementAndGet(this);//only here to decrement
            try {
                PooledObject<K, V> p = searchOrCreate(currentThread);
                if (p != null) recycle(p);
            } catch (Throwable e) {
                this.transferException(e);
            }
        }

        this.servantState = THREAD_WAITING;
    }

    //***************************************************************************************************************//
    //                                         13: Pool Internal classes(0+6)                                        //                                                                              //
    //***************************************************************************************************************//
    private static class ObjectPlainHandleFactory<K, V> {
        protected final BeeObjectPredicate predicate;

        ObjectPlainHandleFactory(BeeObjectPredicate predicate) {
            this.predicate = predicate;
        }

        BeeObjectHandle<K, V> createHandle(PooledObject<K, V> p) throws Exception {
            return new PooledObjectPlainHandle<>(p, predicate);
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
            return new PooledObjectProxyHandle<>(p, predicate, objectProxyClassConstructor);
        }
    }

    private static final class BorrowerThreadLocal<K, V> extends ThreadLocal<WeakReference<Borrower<K, V>>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<Borrower<K, V>> initialValue() {
            return new WeakReference<>(new Borrower<>(Thread.currentThread()));
        }
    }

    static final class FairTransferPolicy<K, V> implements ObjectTransferPolicy<K, V> {
        public int getStateCodeOnRelease() {
            return OBJECT_BORROWED;
        }

        public boolean tryCatch(PooledObject<K, V> p) {
            return p.state == OBJECT_BORROWED;
        }
    }

    static final class CompeteTransferPolicy<K, V> implements ObjectTransferPolicy<K, V> {
        public int getStateCodeOnRelease() {
            return OBJECT_IDLE;
        }

        public boolean tryCatch(PooledObject<K, V> p) {
            return p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_BORROWED);
        }
    }

    private static final class PoolInitAsyncCreateThread<K, V> extends Thread {
        private final int initialSize;
        private final ObjectKeyCategoryPool<K, V> pool;

        PoolInitAsyncCreateThread(int initialSize, ObjectKeyCategoryPool<K, V> pool) {
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
}
