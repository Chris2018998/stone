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
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
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
final class ObjectInstancePool implements Runnable, Cloneable {
    static final AtomicIntegerFieldUpdater<PooledObject> ObjStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledObject.class, "state");
    static final AtomicIntegerFieldUpdater<ObjectInstancePool> ServantStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectInstancePool.class, "servantState");
    private static final Logger Log = LoggerFactory.getLogger(ObjectInstancePool.class);
    private static final AtomicReferenceFieldUpdater<ObjectBorrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(ObjectBorrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<ObjectInstancePool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectInstancePool.class, "poolState");
    private static final AtomicIntegerFieldUpdater<ObjectInstancePool> ServantTryCountUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectInstancePool.class, "servantTryCount");
    private static final Random searchIndexRandom = new Random();
    final KeyedObjectPool ownerPool;

    //clone begin
    private final int maxActiveSize;
    private final String poolMode;
    private final boolean isFairMode;
    private final boolean isCompeteMode;
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
    private final BeeObjectFactory objectFactory;
    private final ObjectPlainHandleFactory handleFactory;
    private final ObjectTransferPolicy transferPolicy;
    private final String poolHostIP;
    private final long poolThreadId;
    private final String poolThreadName;
    private final boolean enableThreadLocal;
    private final BeeObjectMethodFilter methodFilter;
    private final Map<MethodCacheKey, Method> methodMap;
    //clone end
    volatile int servantState;
    volatile int servantTryCount;
    PooledObject[] objectArray;
    ConcurrentLinkedQueue<ObjectBorrower> waitQueue;
    private Object key;
    private String poolName;//owner's poolName + [key.toString()]
    private volatile int poolState;
    private InterruptionSemaphore semaphore;
    private ThreadLocal<WeakReference<ObjectBorrower>> threadLocal;
    private ObjectPoolMonitorVo monitorVo;
    private boolean printRuntimeLog;

    //***************************************************************************************************************//
    //                1: Pool Creation/clone(2)                                                                      //
    //***************************************************************************************************************//
    //method-1.1: constructor for clone
    ObjectInstancePool(BeeObjectSourceConfig config, KeyedObjectPool ownerPool) {
        //step1: copy  primitive type field
        this.ownerPool = ownerPool;
        this.maxActiveSize = config.getMaxActive();
        this.isFairMode = config.isFairMode();
        this.isCompeteMode = !isFairMode;
        this.poolMode = isFairMode ? "fair" : "compete";
        this.enableThreadLocal = config.isEnableThreadLocal();
        this.semaphoreSize = config.getBorrowSemaphoreSize();
        this.maxWaitMs = config.getMaxWait();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(maxWaitMs);//nanoseconds
        this.idleTimeoutMs = config.getIdleTimeout();//milliseconds
        this.holdTimeoutMs = config.getHoldTimeout();//milliseconds
        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.parkTimeForRetryNs = TimeUnit.MILLISECONDS.toNanos(config.getParkTimeForRetry());
        this.validAssumeTime = config.getAliveAssumeTime();
        this.validTestTimeout = config.getAliveTestTimeout();
        this.printRuntimeLog = config.isPrintRuntimeLog();
        this.poolState = POOL_NEW;

        //step2:object type field setting
        this.objectFactory = config.getObjectFactory();
        Class<?>[] objectInterfaces = config.getObjectInterfaces();
        BeeObjectPredicate predicate = config.getObjectPredicate();
        this.methodFilter = config.getObjectMethodFilter();
        this.methodMap = new ConcurrentHashMap<>(1);

        this.transferPolicy = isFairMode ? new FairTransferPolicy() : new CompeteTransferPolicy();
        this.stateCodeOnRelease = transferPolicy.getStateCodeOnRelease();

        if (objectInterfaces != null && objectInterfaces.length > 0)
            this.handleFactory = new ObjectProxyHandleFactory(predicate, this.getClass().getClassLoader(), objectInterfaces, methodFilter);
        else
            this.handleFactory = new ObjectPlainHandleFactory(predicate);

        //step3:pool monitor setting
        Thread currentThread = Thread.currentThread();
        this.poolThreadId = currentThread.getId();
        this.poolThreadName = currentThread.getName();
        String localHostIP = "";
        try {
            localHostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            Log.info("BeeOP({})failed to resolve pool host ip", config.getPoolName());
        } finally {
            this.poolHostIP = localHostIP;
        }
    }

    //method-1.2: create a clone object
    ObjectInstancePool createByClone() throws Exception {
        return (ObjectInstancePool) clone();
    }

    //method-1.3: startup pool
    void startup(String ownerName, Object key, int initSize, boolean async) throws Exception {
        this.key = key;
        this.poolName = ownerName + "-[" + key + "]";
        this.objectArray = new PooledObject[maxActiveSize];
        for (int i = 0; i < maxActiveSize; i++)
            objectArray[i] = new PooledObject(key, objectFactory, methodMap, this.methodFilter, this);

        if (initSize > 0 && !async) this.createInitObjects(initSize, true);
        if (this.enableThreadLocal) this.threadLocal = new BorrowerThreadLocal();
        this.semaphore = new InterruptionSemaphore(semaphoreSize, isFairMode);
        this.waitQueue = new ConcurrentLinkedQueue<>();

        this.servantTryCount = 0;
        this.servantState = THREAD_WAITING;

        if (initSize > 0 && async) new PoolInitAsyncCreateThread(initSize, this).start();
        this.monitorVo = new ObjectPoolMonitorVo(this.poolName, poolHostIP, poolThreadId, poolThreadName, poolMode, maxActiveSize);

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
    //                2: Pooled object create/remove methods(3)                                                      //                                                                                  //
    //***************************************************************************************************************//
    //Method-2.1: create specified size objects to pool,if zero,then try to create one
    void createInitObjects(int initSize, boolean syn) throws Exception {
        if (syn) {
            int index = 0;
            try {
                while (index < initSize) {
                    PooledObject p = objectArray[index];
                    p.state = OBJECT_CREATING;
                    this.fillRawObject(p, OBJECT_IDLE);
                    index++;
                }
            } catch (Throwable e) {
                for (int i = 0; i < index; i++)
                    objectArray[i].onBeforeRemove(DESC_RM_INIT);
                throw e;
            }
        } else {//async creation
            try {
                for (int i = 0; i < initSize; i++) {
                    PooledObject p = objectArray[i];
                    if (ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING))
                        this.fillRawObject(p, OBJECT_USING);
                }
            } catch (Throwable e) {
                Log.warn("Failed to create initial objects by async mode", e);
            }
        }
    }

    //Method-2.2: search one idle Object,if not found,then try to create one
    private PooledObject searchOrCreate() throws Exception {
        final int startIndex = searchIndexRandom.nextInt(this.maxActiveSize);
        int searchIndex = startIndex;
        do {
            PooledObject p = this.objectArray[searchIndex];
            int state = p.state;
            if (state == OBJECT_IDLE) {
                if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING)) {
                    if (this.testOnBorrow(p)) return p;
                } else if (p.state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                    return this.fillRawObject(p, OBJECT_USING);
                }
            } else if (state == OBJECT_CLOSED && ObjStUpd.compareAndSet(p, OBJECT_CLOSED, OBJECT_CREATING)) {
                return this.fillRawObject(p, OBJECT_USING);
            }
            if (++searchIndex == maxActiveSize) searchIndex = 0;
        } while (searchIndex != startIndex);
        return null;
    }

    //Method-2.3: create one pooled object
    private PooledObject fillRawObject(PooledObject p, int state) throws Exception {
        //1: print runtime log of object creation
        if (this.printRuntimeLog)
            Log.info("BeeCP({}))begin to create a raw object", this.poolName);

        Object rawObj = null;
        try {
            p.creatingInfo = new ObjectCreatingInfo();
            rawObj = this.objectFactory.create(this.key);
            if (rawObj == null) {//if blocking interrupt on LockSupport.park in factory,maybe just return a null object?
                if (Thread.interrupted())
                    throw new ObjectGetInterruptedException("Interrupted on creating a raw object by factory");
                throw new ObjectCreateException("Internal error occurred in object factory");
            }

            objectFactory.setDefault(key, rawObj);
            p.setRawObject(state, rawObj);
            if (this.printRuntimeLog)
                Log.info("BeeOP({}))has created a new pooled object:{} with state:{}", this.poolName, p, state);

            return p;
        } catch (Throwable e) {
            p.state = OBJECT_CLOSED;//reset to closed state
            if (rawObj != null) this.objectFactory.destroy(key, rawObj);
            throw e instanceof Exception ? (Exception) e : new ObjectCreateException(e);
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
    public BeeObjectHandle getObjectHandle() throws Exception {
        if (this.poolState != POOL_READY)
            throw new ObjectGetForbiddenException("Access rejected,cause:pool closed or in clearing");

        //1: try to reuse object in thread local
        ObjectBorrower b = null;
        PooledObject p;
        if (this.enableThreadLocal) {
            b = this.threadLocal.get().get();
            if (b != null) {
                p = b.lastUsed;
                if (p != null && p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING)) {
                    if (this.testOnBorrow(p)) return handleFactory.createHandle(p);
                    b.lastUsed = null;
                }
            }
        }

        //2: try to acquire a permit of pool semaphore
        long deadline = System.nanoTime();
        try {
            if (!this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ObjectGetTimeoutException("Waited timeout on pool semaphore");
        } catch (InterruptedException e) {
            throw new ObjectGetInterruptedException("An interruption occurred while waiting on pool semaphore");
        }

        //3: try to search idle one or create new one
        try {
            p = this.searchOrCreate();
        } catch (Exception e) {
            semaphore.release();
            throw e;
        }
        //3.1: check searched result
        final boolean hasCached = b != null;
        if (p != null) {
            semaphore.release();
            if (this.enableThreadLocal) {
                if (hasCached)
                    b.lastUsed = p;
                else
                    this.threadLocal.set(new WeakReference<>(new ObjectBorrower(p)));
            }
            return handleFactory.createHandle(p);
        }

        //4: add the borrower to wait queue
        if (hasCached)
            b.state = null;
        else
            b = new ObjectBorrower();
        this.waitQueue.offer(b);
        BeeObjectException cause = null;
        deadline += this.maxWaitNs;

        //5: self-spin to get transferred object
        do {
            final Object s = b.state;//possible values: PooledObject,Throwable,null
            if (s instanceof PooledObject) {
                p = (PooledObject) s;
                if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                    semaphore.release();
                    waitQueue.remove(b);
                    if (this.enableThreadLocal) {
                        b.lastUsed = p;
                        if (!hasCached) this.threadLocal.set(new WeakReference<>(b));
                    }
                    return handleFactory.createHandle(p);
                }
            } else if (s instanceof Throwable) {//here: s must be throwable object
                semaphore.release();
                waitQueue.remove(b);
                throw s instanceof Exception ? (Exception) s : new ObjectGetException((Throwable) s);
            }

            //reach here:s==null or s is a PooledObject
            if (cause != null) {
                BorrowStUpd.compareAndSet(b, s, cause);
            } else {
                if (s != null) b.state = null;
                final long t = deadline - System.nanoTime();
                if (t > 0L) {
                    if (this.servantTryCount > 0 && this.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(this, THREAD_WAITING, THREAD_WORKING))
                        ownerPool.submitServantTask(this);

                    LockSupport.parkNanos(t);//park exit:1:get transfer 2:timeout 3:interrupted
                    if (Thread.interrupted())
                        cause = new ObjectGetInterruptedException("An interruption occurred while waiting for a released object");
                } else {//timeout
                    cause = new ObjectGetTimeoutException("Waited timeout for a released object");
                }
            }
        } while (true);//while
    }

    //Method-3.2: return object to pool after borrower end of use object
    void recycle(PooledObject p) {
        if (isCompeteMode) p.state = OBJECT_IDLE;

        for (ObjectBorrower b : waitQueue) {
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
     * Method-3.3: terminate a Pooled object
     *
     * @param p      to be closed and removed
     * @param reason is a cause for be aborted
     */
    void abort(PooledObject p, String reason) {
        p.onBeforeRemove(reason);
        this.tryWakeupServantThread();
    }

    /**
     * Method-3.4: when object create failed,creator thread will transfer caused exception to one waiting borrower,
     * which will exit wait and throw this exception.
     *
     * @param e: transfer Exception to waiter
     */
    private void transferException(Throwable e) {
        for (ObjectBorrower b : waitQueue) {
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    //Method-3.5: check object alive state,if not alive then remove it from pool
    private boolean testOnBorrow(PooledObject p) {
        try {
            if (System.currentTimeMillis() - p.lastAccessTime >= this.validAssumeTime && !this.objectFactory.isValid(key, p.raw, this.validTestTimeout)) {
                p.onBeforeRemove(DESC_RM_BAD);
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
            ownerPool.submitServantTask(this);
        }
    }

    //Method-4.2: servant method driven by executor in key pool
    public void run() {
        while (servantState == THREAD_WORKING) {
            int c = servantTryCount;
            if (c <= 0 || (waitQueue.isEmpty() && ServantTryCountUpd.compareAndSet(this, c, 0))) break;
            ServantTryCountUpd.decrementAndGet(this);

            try {
                PooledObject p = searchOrCreate();
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
            Log.info("BeeOP({})-before idle clear,idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }

        //step2: attempt to interrupt timeout creation
        this.interruptObjectCreating(true);

        //step3: remove idle timeout and hold timeout
        for (PooledObject p : this.objectArray) {
            int state = p.state;
            if (state == OBJECT_IDLE && this.semaphore.availablePermits() == this.semaphoreSize) {//no borrowers on semaphore
                boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime - this.idleTimeoutMs >= 0L;
                if (isTimeoutInIdle && ObjStUpd.compareAndSet(p, state, OBJECT_CLOSED)) {//need close idle
                    p.onBeforeRemove(DESC_RM_IDLE);
                    this.tryWakeupServantThread();
                }
            } else if (state == OBJECT_USING && supportHoldTimeout) {
                if (System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L) {//hold timeout
                    BeeObjectHandle handleInUsing = p.handleInUsing;
                    if (handleInUsing != null) tryCloseObjectHandle(handleInUsing);
                }
            }
        }

        //step4: print pool info after idle clean
        if (this.printRuntimeLog) {
            BeeObjectPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeOP({})-after idle clear,idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //***************************************************************************************************************//
    //                                      6: Pooled objects clear(2)                                               //                                                                                  //
    //***************************************************************************************************************//
    //Method-6.1: remove all object from pool
    boolean clear(boolean forceCloseUsing) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            Log.info("BeeOP({})begin to clear all objects", this.poolName);
            this.clear(forceCloseUsing, DESC_RM_CLEAR);
            Log.info("BeeOP({})has clear all objects", this.poolName);
            this.poolState = POOL_READY;// restore state;
            Log.info("BeeOP({})pool has cleared all objects", this.poolName);
            return true;
        } else {
            return false;
        }
    }

    //Method-6.2: remove all connections from pool
    private void clear(boolean forceCloseUsing, String removeReason) {
        //1:interrupt waiters on semaphore
        this.semaphore.interruptQueuedWaitThreads();
        if (!this.waitQueue.isEmpty()) {
            PoolInClearingException clearException = new PoolInClearingException("Object pool was in clearing");
            while (!this.waitQueue.isEmpty()) this.transferException(clearException);
        }

        //2: interrupt all threads of object creation
        this.interruptObjectCreating(false);

        //3:clear all connections
        int closedCount;
        while (true) {
            closedCount = 0;
            for (PooledObject p : this.objectArray) {
                final int state = p.state;
                if (state == OBJECT_IDLE) {
                    if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_CLOSED)) {
                        closedCount++;
                        p.onBeforeRemove(removeReason);
                    }
                } else if (state == OBJECT_USING) {
                    BeeObjectHandle handleInUsing = p.handleInUsing;
                    if (handleInUsing != null) {
                        if (forceCloseUsing || (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L))
                            tryCloseObjectHandle(handleInUsing);
                    } else {
                        p.onBeforeRemove(removeReason);
                    }
                } else if (state == OBJECT_CLOSED) {
                    closedCount++;
                }
            } // for

            if (closedCount == this.maxActiveSize) break;
            LockSupport.parkNanos(this.parkTimeForRetryNs);
        } // while

        if (this.printRuntimeLog) {
            BeeObjectPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeOP({})idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
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
    public void close(boolean forceCloseUsing) {
        do {
            int poolStateCode = this.poolState;
            if (poolStateCode == POOL_CLOSED || poolStateCode == POOL_CLOSING) return;
            if (poolStateCode == POOL_NEW && PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_CLOSED)) return;
            if (poolStateCode == POOL_STARTING || poolStateCode == POOL_CLEARING) {
                LockSupport.parkNanos(this.parkTimeForRetryNs);//delay and retry
            } else if (PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSING)) {//poolStateCode == POOL_NEW || poolStateCode == POOL_READY
                Log.info("BeeOP({})begin to shutdown", this.poolName);
                this.clear(forceCloseUsing, DESC_RM_DESTROY);

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

    String getPoolHostIP() {
        return poolHostIP;
    }

    long getPoolThreadId() {
        return poolThreadId;
    }

    String getPoolThreadName() {
        return poolThreadName;
    }

    long getParkTimeForRetryNs() {
        return this.parkTimeForRetryNs;
    }

    boolean isPrintRuntimeLog() {
        return this.printRuntimeLog;
    }

    void setPrintRuntimeLog(boolean indicator) {
        printRuntimeLog = indicator;
    }

    private int getTotalSize() {
        int size = 0;
        for (PooledObject p : objectArray) {
            int state = p.state;
            if (state == OBJECT_IDLE || state == OBJECT_USING) size++;
        }
        return size;
    }

    private int getIdleSize() {
        int idleSize = 0;
        for (PooledObject p : this.objectArray)
            if (p.state == OBJECT_IDLE) idleSize++;
        return idleSize;
    }

    private int getTransferWaitingSize() {
        int size = 0;
        for (ObjectBorrower borrower : this.waitQueue) {
            if (borrower.state == null) size++;
        }
        return size;
    }

    BeeObjectPoolMonitorVo getPoolMonitorVo() {
        int usingSize = 0, idleSize = 0;
        int creatingCount = 0, creatingTimeoutCount = 0;
        for (PooledObject p : objectArray) {
            int state = p.state;
            if (state == OBJECT_USING) usingSize++;
            if (state == OBJECT_IDLE) idleSize++;
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) {
                creatingCount++;
                if (System.nanoTime() - creatingInfo.creatingStartTime >= maxWaitNs)
                    creatingTimeoutCount++;
            }
        }
        monitorVo.setPoolState(poolState);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(usingSize);
        monitorVo.setCreatingCount(creatingCount);
        monitorVo.setCreatingTimeoutCount(creatingTimeoutCount);
        monitorVo.setSemaphoreWaitingSize(this.semaphore.getQueueLength());
        monitorVo.setTransferWaitingSize(getTransferWaitingSize());
        return this.monitorVo;
    }

    public int getObjectCreatingCount() {
        int count = 0;
        for (PooledObject p : objectArray) {
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null) count++;
        }
        return count;
    }

    public int getObjectCreatingTimeoutCount() {
        int count = 0;
        for (PooledObject p : objectArray) {
            ObjectCreatingInfo creatingInfo = p.creatingInfo;
            if (creatingInfo != null && System.nanoTime() - creatingInfo.creatingStartTime >= maxWaitNs)
                count++;
        }
        return count;
    }

    public Thread[] interruptObjectCreating(boolean onlyInterruptTimeout) {
        if (this.printRuntimeLog)
            Log.info("BeeCP({})attempt to interrupt object creation,only for timeout:{}", this.poolName, onlyInterruptTimeout);

        ArrayList<Thread> threads = new ArrayList<>(this.semaphoreSize);
        if (onlyInterruptTimeout) {
            for (PooledObject p : objectArray) {
                ObjectCreatingInfo creatingInfo = p.creatingInfo;
                if (creatingInfo != null && System.nanoTime() - creatingInfo.creatingStartTime >= maxWaitNs) {
                    creatingInfo.creatingThread.interrupt();
                    threads.add(creatingInfo.creatingThread);
                }
            }
        } else {
            for (PooledObject p : objectArray) {
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
    private static class ObjectPlainHandleFactory {
        protected final BeeObjectPredicate predicate;

        ObjectPlainHandleFactory(BeeObjectPredicate predicate) {
            this.predicate = predicate;
        }

        BeeObjectHandle createHandle(PooledObject p) {
            return new PooledObjectPlainHandle(p, predicate);
        }
    }

    private static class ObjectProxyHandleFactory extends ObjectPlainHandleFactory {
        private final ClassLoader poolClassLoader;
        private final Class<?>[] objectInterfaces;
        private final BeeObjectMethodFilter methodFilter;

        ObjectProxyHandleFactory(BeeObjectPredicate predicate, ClassLoader poolClassLoader, Class<?>[] objectInterfaces, BeeObjectMethodFilter methodFilter) {
            super(predicate);
            this.poolClassLoader = poolClassLoader;
            this.objectInterfaces = objectInterfaces;
            this.methodFilter = methodFilter;
        }

        BeeObjectHandle createHandle(PooledObject p) {
            return new PooledObjectProxyHandle(p, predicate, poolClassLoader, objectInterfaces, methodFilter);
        }
    }

    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<ObjectBorrower>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<ObjectBorrower> initialValue() {
            return new WeakReference<>(new ObjectBorrower());
        }
    }

    static final class FairTransferPolicy implements ObjectTransferPolicy {
        public int getStateCodeOnRelease() {
            return OBJECT_USING;
        }

        public boolean tryCatch(PooledObject p) {
            return p.state == OBJECT_USING;
        }
    }

    static final class CompeteTransferPolicy implements ObjectTransferPolicy {
        public int getStateCodeOnRelease() {
            return OBJECT_IDLE;
        }

        public boolean tryCatch(PooledObject p) {
            return p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING);
        }
    }

    private static final class PoolInitAsyncCreateThread extends Thread {
        private final int initialSize;
        private final ObjectInstancePool pool;

        PoolInitAsyncCreateThread(int initialSize, ObjectInstancePool pool) {
            this.initialSize = initialSize;
            this.pool = pool;
        }

        public void run() {
            try {
                pool.createInitObjects(initialSize, false);
                pool.servantTryCount = pool.objectArray.length;
                if (!pool.waitQueue.isEmpty() && pool.servantState == THREAD_WAITING && ServantStateUpd.compareAndSet(pool, THREAD_WAITING, THREAD_WORKING)) {
                    pool.ownerPool.submitServantTask(pool);
                }
            } catch (Throwable e) {
                //do nothing
            }
        }
    }
}
