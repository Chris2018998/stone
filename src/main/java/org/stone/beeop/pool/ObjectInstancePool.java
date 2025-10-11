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
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;
import org.stone.tools.extension.InterruptionSemaphore;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * Object instance Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ObjectInstancePool<K, V> implements Runnable, Cloneable {
    static final AtomicIntegerFieldUpdater<PooledObject> ObjStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledObject.class, "state");
    static final AtomicIntegerFieldUpdater<ObjectInstancePool> ServantStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectInstancePool.class, "servantState");
    private static final Logger Log = LoggerFactory.getLogger(ObjectInstancePool.class);
    private static final AtomicReferenceFieldUpdater<ObjectBorrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(ObjectBorrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<ObjectInstancePool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectInstancePool.class, "poolState");
    private static final AtomicIntegerFieldUpdater<ObjectInstancePool> ServantTryCountUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectInstancePool.class, "servantTryCount");
    final KeyedObjectPool<K, V> parentPool;

    //clone begin
    private final String poolMode;
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

    //* create objects to be managed in pools
    private final BeeObjectFactory<K, V> objectFactory;
    //* create proxy to wrap pooled objects and return to borrowers as result
    private final ObjectPlainHandleFactory<K, V> handleFactory;
    //* a tool to transfer released objects to waiters
    private final ObjectTransferPolicy<K, V> transferPolicy;
    //an indicator to enable or disable pool thread local which can cache last used object for borrowers
    private final boolean enableThreadLocal;
    //a map store methods of pooled objects
    private final Map<MethodCacheKey, Method> methodMap;
    //clone end

    //state of servant thread,which is used to search idle objects or create objects for borrowers in wait queue
    volatile int servantState;
    //retry count of servant thread to work
    volatile int servantTryCount;

    //an array store pooled objects
    PooledObject<K, V>[] objectArray;
    //A wait queue,borrowers offer them-self into it when all objects are borrowed out from pool
    ConcurrentLinkedQueue<ObjectBorrower<K, V>> waitQueue;

    //pooled key
    private K key;
    //pooled name
    private String poolName;//owner's poolName + [key.toString()]
    //pool state
    private volatile int poolState;
    //pool semaphore
    private InterruptionSemaphore semaphore;
    //thread local to cache last borrowed objects for borrowers
    private ThreadLocal<WeakReference<ObjectBorrower<K, V>>> threadLocal;
    //pool monitor vo
    private ObjectPoolMonitorVo monitorVo;
    //a
    private boolean printRuntimeLog;

    //***************************************************************************************************************//
    //                1: Pool Creation/clone(2)                                                                      //
    //***************************************************************************************************************//
    //method-1.1: constructor for default sub pool
    ObjectInstancePool(BeeObjectSourceConfig<K, V> config, KeyedObjectPool<K, V> ownerPool, Constructor<?> objectProxyClassConstructor) {
        //step1: copy  primitive type field
        this.parentPool = ownerPool;

        this.maxActiveSize = config.getMaxActive();
        this.isFairMode = config.isFairMode();
        this.isCompeteMode = !isFairMode;
        this.poolMode = isFairMode ? "fair" : "compete";
        this.enableThreadLocal = config.isUseThreadLocal();
        this.semaphoreSize = config.getSemaphoreSize();

        this.maxWaitMs = config.getMaxWait();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(maxWaitMs);//nanoseconds
        this.idleTimeoutMs = config.getIdleTimeout();
        this.holdTimeoutMs = config.getHoldTimeout();

        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.parkTimeForRetryNs = TimeUnit.MILLISECONDS.toNanos(config.getParkTimeForRetry());
        this.validAssumeTime = config.getAliveAssumeTime();
        this.validTestTimeout = config.getAliveTestTimeout();
        this.printRuntimeLog = config.isPrintRuntimeLogs();
        this.poolState = POOL_NEW;

        //step2:object type field setting
        this.objectFactory = config.getObjectFactory();
        BeeObjectPredicate predicate = config.getPredicate();
        this.methodMap = new ConcurrentHashMap<>(1);

        this.transferPolicy = isFairMode ? new FairTransferPolicy<>() : new CompeteTransferPolicy<>();
        this.stateCodeOnRelease = transferPolicy.getStateCodeOnRelease();

        if (objectProxyClassConstructor != null)
            this.handleFactory = new ObjectProxyHandleFactory<>(predicate, objectProxyClassConstructor);
        else
            this.handleFactory = new ObjectPlainHandleFactory<>(predicate);
    }

    //method-1.2: create a clone object
    ObjectInstancePool<K, V> createByClone() throws Exception {
        return (ObjectInstancePool<K, V>) clone();
    }

    //method-1.3: startup pool
    void startup(String ownerName, K key, int initSize, boolean async) throws Exception {
        this.key = key;
        this.poolName = ownerName + "-[" + key + "]";
        this.objectArray = new PooledObject[maxActiveSize];
        for (int i = 0; i < maxActiveSize; i++)
            objectArray[i] = new PooledObject(key, objectFactory, methodMap, this);

        if (initSize > 0 && !async) this.createInitObjects(initSize, true);
        if (this.enableThreadLocal) this.threadLocal = new BorrowerThreadLocal<>();
        this.semaphore = new InterruptionSemaphore(semaphoreSize, isFairMode);
        this.waitQueue = new ConcurrentLinkedQueue<>();

        this.servantTryCount = 0;
        this.servantState = THREAD_WAITING;//initial state

        if (initSize > 0 && async) new PoolInitAsyncCreateThread<>(initSize, this).start();
        this.monitorVo = new ObjectPoolMonitorVo(this.poolName, poolMode, maxActiveSize);

        this.poolState = POOL_READY;
        Log.info("BeeOP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms",
                this.poolName,
                this.poolMode,
                initSize,
                this.maxActiveSize,
                this.semaphoreSize,
                this.maxWaitMs);
    }

    //***************************************************************************************************************//
    //                2: Pooled objects creation/remove methods(3)                                                      //                                                                                  //
    //***************************************************************************************************************//
    //Method-2.1: create specified size objects to pool
    void createInitObjects(int initSize, boolean syn) throws Exception {
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
                    objectArray[i].onRemove(DESC_RM_INIT);
                throw e;
            } else {
                Log.warn("Failed to create initial objects during async mode", e);
            }
        }
    }

    //Method-2.2: search one idle Object,if not found,then try to create one
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

    //Method-2.3: create one pooled object
    private PooledObject<K, V> fillRawObject(PooledObject<K, V> p, int state, Thread creatingThread) throws Exception {
        //1: print runtime log of object creation
        if (this.printRuntimeLog)
            Log.info("BeeCP({}))begin to create a raw object", this.poolName);

        V rawObj = null;
        try {
            p.creatingInfo = new ObjectCreatingInfo(creatingThread);
            rawObj = this.objectFactory.create(this.key);
            if (rawObj == null) {//if blocking interrupt on LockSupport.park in factory,maybe just return a null object?
                if (creatingThread.isInterrupted() && Thread.interrupted())
                    throw new ObjectGetInterruptedException("Interrupted on creating a raw object by factory");
                throw new ObjectCreateException("Internal error occurred in object factory");
            }

            objectFactory.setDefault(key, rawObj);
            p.setRawObject(state, rawObj);
            if (this.printRuntimeLog)
                Log.info("BeeOP({})has created a new pooled object:{} with state:{}", this.poolName, p, state);

            return p;
        } catch (Throwable e) {
            p.state = OBJECT_CLOSED;//reset to closed state
            if (rawObj != null) this.objectFactory.destroy(key, rawObj);
            throw new ObjectCreateException(e);
        } finally {
            p.creatingInfo = null;
        }
    }

    //***************************************************************************************************************//
    //                  3: Pooled object borrow and release methods(5)                                               //                                                                                  //
    //***************************************************************************************************************//

    /**
     * Method-3.1:borrow one object from pool,if search one idle object in pool,then try to catch it and return it
     * if not search,then wait until other borrowers release objects or wait timeout
     *
     * @return pooled object,
     * @throws Exception if pool is closed or waiting timeout,then throw exception
     */
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Pool has been closed or in clearing");

        //1: try to reuse object in thread local
        ObjectBorrower<K, V> b = null;
        PooledObject<K, V> p;
        if (this.enableThreadLocal) {
            b = this.threadLocal.get().get();
            if (b != null) {
                p = b.lastUsed;
                if (p != null && p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_BORROWED)) {
                    if (this.testOnBorrow(p)) return handleFactory.createHandle(p);
                    b.lastUsed = null;//clear cached bad object
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
                        if (this.enableThreadLocal) {
                            if (b != null)
                                b.lastUsed = p;
                            else
                                this.threadLocal.set(new WeakReference<>(new ObjectBorrower<>(borrowThread, p)));
                        }
                        return handleFactory.createHandle(p);
                    }

                    //4: add the borrower to wait queue
                    if (b != null) {
                        b.state = null;
                    } else {
                        b = new ObjectBorrower<>(borrowThread);
                        if (this.enableThreadLocal) this.threadLocal.set(new WeakReference<>(b));
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

    //Method-3.2: handle timeout and interruption in spin
    private BeeObjectHandle<K, V> handleTimeoutAndInterruption(boolean isTimeout, Object s, ObjectBorrower<K, V> b) throws Exception {
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

    //Method-3.3: return object to pool after borrower end of use object
    void recycle(PooledObject<K, V> p) {
        if (isCompeteMode) p.state = OBJECT_IDLE;

        for (ObjectBorrower<K, V> b : waitQueue) {
            if (p.state != stateCodeOnRelease) return;
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, p)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }

        if (isFairMode) p.state = OBJECT_IDLE;
        tryWakeupServantThread();
    }

    /**
     * Method-3.4: terminate a Pooled object
     *
     * @param p      to be closed and removed
     * @param reason is a cause for be aborted
     */
    void abort(PooledObject<K, V> p, String reason) {
        p.onRemove(reason);
        this.tryWakeupServantThread();
    }

    /**
     * Method-3.4: when object create failed,creator thread will transfer caused exception to one waiting borrower,
     * which will exit wait and throw this exception.
     *
     * @param e: transfer Exception to waiter
     */
    private void transferException(Throwable e) {
        for (ObjectBorrower<K, V> b : waitQueue) {
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    //Method-3.5: check object alive state,if not alive then remove it from pool
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
            if (this.printRuntimeLog)
                Log.warn("BeeOP({})alive test failed on a borrowed object", this.poolName, e);
        }
        return false;
    }

    //***************************************************************************************************************//
    //                          4: Async Servant(2)                                                                  //                                                                                  //
    //***************************************************************************************************************//
    //Method-4.1: try to wakeup servant thread to work if it in waiting
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

    //Method-4.2: servant method driven by executor in key pool
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
    //                          5: Idle-timeout and hold-timeout clear                                               //                                                                                  //
    //***************************************************************************************************************//
    //Method-5.1: clear idle-timeout pooled objects and hold-time objects,this method will be called by ScheduledThreadPoolExecutor in key pool
    void closeIdleTimeout() {
        //step1: print pool info before clean
        if (this.printRuntimeLog) {
            BeeObjectPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeOP({})-before idle clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2: attempt to interrupt timeout creation
        this.interruptObjectCreating(true);

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
        if (this.printRuntimeLog) {
            BeeObjectPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeOP({})-after idle clear,idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                      6: Pooled objects clear(2)                                               //                                                                                  //
    //***************************************************************************************************************//
    //Method-6.1: remove all object from pool
    boolean clear(boolean forceRecycleBorrowed) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            Log.info("BeeOP({})begin to clear all objects", this.poolName);
            this.clear(forceRecycleBorrowed, DESC_RM_CLEAR);
            Log.info("BeeOP({})has clear all objects", this.poolName);
            this.poolState = POOL_READY;// restore state;
            Log.info("BeeOP({})pool has cleared all objects", this.poolName);
            return true;
        } else {
            return false;
        }
    }

    //Method-6.2: remove all connections from pool
    private void clear(boolean forceRecycleBorrowed, String removeReason) {
        //1: interrupt waiters on semaphore
        this.semaphore.interruptQueuedWaitThreads();
        //2: interrupt all threads waits on lock or blocking in factory.create method call
        this.interruptObjectCreating(false);
        //3: transfer exception to waiter in queue
        if (!this.waitQueue.isEmpty()) {
            PoolInClearingException clearException = new PoolInClearingException("Object pool was in clearing");
            while (!this.waitQueue.isEmpty()) this.transferException(clearException);
        }

        //4:clear all connections
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

        if (this.printRuntimeLog) {
            BeeObjectPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeOP({})idle:{},borrowed:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getBorrowedSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                      7: Pooled close (2)                                                      //                                                                                  //
    //***************************************************************************************************************//
    //Method-7.1: closed check
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //Method-7.2: close pool
    public void close(boolean forceRecycleBorrowed) {
        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_NEW && PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_CLOSED)) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_CLEARING) {
                LockSupport.parkNanos(this.parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                Log.info("BeeOP({})begin to shutdown", this.poolName);
                this.clear(forceRecycleBorrowed, DESC_RM_DESTROY);

                this.poolState = POOL_CLOSED;
                Log.info("BeeOP({})has shutdown", this.poolName);
                break;
            } else {//pool State == POOL_CLOSING
                break;
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                                       8: Pool monitor(9)                                                      //                                                                                  //
    //***************************************************************************************************************//
    String getPoolName() {
        return poolName;
    }

    String getPoolMode() {
        return poolMode;
    }

    long getMaxWaitNs() {
        return this.maxWaitNs;
    }

    long getParkTimeForRetryNs() {
        return this.parkTimeForRetryNs;
    }

    boolean isPrintRuntimeLog() {
        return this.printRuntimeLog;
    }

    void setPrintRuntimeLog(boolean enable) {
        printRuntimeLog = enable;
    }

    private int getTotalSize() {
        int size = 0;
        for (PooledObject<K, V> p : objectArray) {
            int state = p.state;
            if (state == OBJECT_IDLE || state == OBJECT_BORROWED) size++;
        }
        return size;
    }

    private int getIdleSize() {
        int idleSize = 0;
        for (PooledObject<K, V> p : this.objectArray)
            if (p.state == OBJECT_IDLE) idleSize++;
        return idleSize;
    }

    private int getTransferWaitingSize() {
        int size = 0;
        for (ObjectBorrower<K, V> borrower : this.waitQueue) {
            if (borrower.state == null) size++;
        }
        return size;
    }

    BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int borrowedSize = 0, idleSize = 0;
        int creatingCount = 0, creatingTimeoutCount = 0;
        int semaphoreWaitingSize = this.semaphore.getQueueLength();
        int transferWaitingSize = this.getTransferWaitingSize();

        for (PooledObject<K, V> p : objectArray) {
            int state = p.state;
            if (state == OBJECT_BORROWED) borrowedSize++;
            if (state == OBJECT_IDLE) idleSize++;
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) {
                creatingCount++;
                if (System.currentTimeMillis() - creatingInfo.creatingStartTime - maxWaitMs >= 0L)
                    creatingTimeoutCount++;
            }
        }
        monitorVo.setKeySize(1);
        monitorVo.setPoolState(poolState);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setBorrowedSize(borrowedSize);
        monitorVo.setCreatingCount(creatingCount);
        monitorVo.setCreatingTimeoutCount(creatingTimeoutCount);
        monitorVo.setSemaphoreWaitingSize(semaphoreWaitingSize);
        monitorVo.setTransferWaitingSize(transferWaitingSize);
        return this.monitorVo;
    }

    public int getObjectCreatingCount() {
        int count = 0;
        for (PooledObject<K, V> p : objectArray) {
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) count++;
        }
        return count;
    }

    public int getObjectCreatingTimeoutCount() {
        int count = 0;
        for (PooledObject<K, V> p : objectArray) {
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null && System.currentTimeMillis() - creatingInfo.creatingStartTime - maxWaitMs >= 0L)
                count++;
        }
        return count;
    }

    public Thread[] interruptObjectCreating(boolean onlyInterruptTimeout) {
        if (this.printRuntimeLog)
            Log.info("BeeCP({})attempt to interrupt object creation,only for timeout:{}", this.poolName, onlyInterruptTimeout);

        ArrayList<Thread> threads = new ArrayList<>(this.semaphoreSize);
        if (onlyInterruptTimeout) {
            for (PooledObject<K, V> p : objectArray) {
                ObjectCreatingInfo creatingInfo = p.creatingInfo;
                if (creatingInfo != null && System.currentTimeMillis() - creatingInfo.creatingStartTime - maxWaitMs >= 0L) {
                    creatingInfo.creatingThread.interrupt();
                    threads.add(creatingInfo.creatingThread);
                }
            }
        } else {
            for (PooledObject<K, V> p : objectArray) {
                ObjectCreatingInfo creatingInfo = p.creatingInfo;
                if (creatingInfo != null) {
                    creatingInfo.creatingThread.interrupt();
                    threads.add(creatingInfo.creatingThread);
                }
            }
        }

        return threads.toArray(new Thread[0]);
    }

    //***************************************************************************************************************//
    //                                       9: Inner Classes(6)                                                     //                                                                                  //
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

    private static final class BorrowerThreadLocal<K, V> extends ThreadLocal<WeakReference<ObjectBorrower<K, V>>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<ObjectBorrower<K, V>> initialValue() {
            return new WeakReference<>(new ObjectBorrower<>(Thread.currentThread()));
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
        private final ObjectInstancePool<K, V> pool;

        PoolInitAsyncCreateThread(int initialSize, ObjectInstancePool<K, V> pool) {
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
