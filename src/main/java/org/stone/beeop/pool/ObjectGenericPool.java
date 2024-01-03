/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beeop.*;
import org.stone.beeop.pool.exception.*;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Object Generic Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ObjectGenericPool implements Runnable, Cloneable {
    private static final AtomicIntegerFieldUpdater<PooledObject> ObjStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledObject.class, "state");
    private static final AtomicReferenceFieldUpdater<ObjectBorrower, Object> BorrowStUpd = ReferenceFieldUpdaterImpl.newUpdater(ObjectBorrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<ObjectGenericPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(ObjectGenericPool.class, "poolState");
    private static final Logger Log = LoggerFactory.getLogger(ObjectGenericPool.class);

    //clone begin
    private final String poolHostIP;
    private final long poolThreadId;
    private final String poolThreadName;
    private final String poolMode;
    private final int poolInitSize;
    private final int poolMaxSize;
    private final boolean isFairMode;
    private final boolean isCompeteMode;
    private final int semaphoreSize;
    private final long maxWaitNs;//nanoseconds
    private final long idleTimeoutMs;//milliseconds
    private final long holdTimeoutMs;//milliseconds
    private final boolean supportHoldTimeout;
    private final int stateCodeOnRelease;
    private final long validAssumeTime;//milliseconds
    private final int validTestTimeout;//seconds
    private final long delayTimeForNextClearNs;//nanoseconds
    private final RawObjectFactory objectFactory;
    private final PooledObject templatePooledObject;
    private final ObjectTransferPolicy transferPolicy;
    private final ObjectHandleFactory handleFactory;
    private final KeyedObjectPool parentPool;
    private boolean printRuntimeLog;
    //clone end

    //set in clone method
    private Object key;
    private String poolName;//parent's poolName + [key.toString()]
    private volatile int poolState;
    private PoolSemaphore semaphore;
    private AtomicInteger servantState;
    private AtomicInteger servantTryCount;
    private ReentrantLock pooledArrayLock;
    private volatile PooledObject[] pooledArray;
    private ThreadLocal<WeakReference<ObjectBorrower>> threadLocal;
    private ConcurrentLinkedQueue<ObjectBorrower> waitQueue;
    private ObjectPoolMonitorVo monitorVo;

    //***************************************************************************************************************//
    //                1: Pool Creation/clone(2)                                                                      //
    //***************************************************************************************************************//
    //method-1.1: constructor for clone
    ObjectGenericPool(BeeObjectSourceConfig config, KeyedObjectPool keyedObjectPool) {
        //step1: primitive type field setting
        this.poolInitSize = config.getInitialSize();
        this.poolMaxSize = config.getMaxActive();
        this.isFairMode = config.isFairMode();
        this.isCompeteMode = !isFairMode;
        this.poolMode = isFairMode ? "fair" : "compete";
        this.semaphoreSize = config.getBorrowSemaphoreSize();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(config.getMaxWait());//nanoseconds
        this.idleTimeoutMs = config.getIdleTimeout();//milliseconds
        this.holdTimeoutMs = config.getHoldTimeout();//milliseconds
        this.supportHoldTimeout = holdTimeoutMs > 0L;
        this.delayTimeForNextClearNs = TimeUnit.MILLISECONDS.toNanos(config.getDelayTimeForNextClear());
        this.validAssumeTime = config.getValidAssumeTime();
        this.validTestTimeout = config.getValidTestTimeout();
        this.printRuntimeLog = config.isPrintRuntimeLog();
        this.poolState = POOL_NEW;

        //step2:object type field setting
        this.parentPool = keyedObjectPool;
        this.objectFactory = config.getObjectFactory();
        Class[] objectInterfaces = config.getObjectInterfaces();
        this.transferPolicy = isFairMode ? new FairTransferPolicy() : new CompeteTransferPolicy();
        this.stateCodeOnRelease = transferPolicy.getStateCodeOnRelease();
        this.templatePooledObject = new PooledObject(objectFactory, objectInterfaces, config.getObjectMethodFilter(),
                new ConcurrentHashMap<ObjectMethodCacheKey, Method>(16));

        if (objectInterfaces != null && objectInterfaces.length > 0)
            this.handleFactory = new ObjectReflectHandleFactory();
        else
            this.handleFactory = new ObjectHandleFactory();

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

    //method-1.2: creation from clone and init(this method called by clone object in keyed pool)
    ObjectGenericPool createByClone(Object key, String parentName, int initSize, boolean async) throws Exception {
        final ObjectGenericPool p = (ObjectGenericPool) clone();
        p.key = key;
        p.poolName = parentName + "-[" + key + "]";
        p.pooledArray = new PooledObject[0];
        p.pooledArrayLock = new ReentrantLock();
        if (initSize > 0 && !async) p.createInitObjects(poolInitSize, true);

        p.threadLocal = new BorrowerThreadLocal();
        p.semaphore = new PoolSemaphore(this.semaphoreSize, isFairMode);
        p.waitQueue = new ConcurrentLinkedQueue<ObjectBorrower>();
        p.servantState = new AtomicInteger(THREAD_WAITING);
        p.servantTryCount = new AtomicInteger(0);
        if (initSize > 0 && async) new PoolInitAsynCreateThread(initSize, p).start();
        p.monitorVo = new ObjectPoolMonitorVo(poolHostIP, poolThreadId, poolThreadName, poolName, poolMode, poolMaxSize);

        p.poolState = POOL_READY;
        Log.info("BeeOP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ns",
                p.poolName,
                poolMode,
                p.pooledArray.length,
                p.poolMaxSize,
                p.semaphoreSize,
                p.maxWaitNs);
        return p;
    }

    //***************************************************************************************************************//
    //                2: Pooled object create/remove methods(3)                                                      //                                                                                  //
    //***************************************************************************************************************//
    //Method-2.1: create specified size objects to pool,if zero,then try to create one
    private void createInitObjects(int initSize, boolean syn) throws Exception {
        pooledArrayLock.lock();
        try {
            for (int i = 0; i < initSize; i++)
                this.createPooledEntry(OBJECT_IDLE);
        } catch (Throwable e) {
            for (PooledObject pooledEntry : this.pooledArray)
                this.removePooledEntry(pooledEntry, DESC_RM_INIT);

            if (syn) {//throw exception on syn mode
                if (e instanceof Exception)
                    throw (Exception) e;
                else
                    throw new PoolInitializedException(e);
            } else {
                Log.warn("Failed to create objects on pool initialization,cause:" + e);
            }
        } finally {
            pooledArrayLock.unlock();
        }
    }

    //Method-2.2: create one pooled object
    private PooledObject createPooledEntry(int state) throws Exception {
        //1:try to acquire lock
        try {
            if (!this.pooledArrayLock.tryLock(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ObjectCreateException("Pooled object create timeout at lock");
        } catch (InterruptedException e) {
            throw new ObjectCreateException("Pooled object create interrupted at lock");
        }

        //2:try to create a pooled object
        try {
            int l = this.pooledArray.length;
            if (l < this.poolMaxSize) {
                if (this.printRuntimeLog)
                    Log.info("BeeOP({}))begin to create a new pooled object,state:{}", this.poolName, state);

                Object rawObj = null;
                try {
                    rawObj = this.objectFactory.create(this.key);
                    PooledObject p = this.templatePooledObject.setDefaultAndCopy(key, rawObj, state, this);
                    if (this.printRuntimeLog)
                        Log.info("BeeOP({}))has created a new pooled object:{},state:{}", this.poolName, p, state);
                    PooledObject[] arrayNew = new PooledObject[l + 1];
                    System.arraycopy(this.pooledArray, 0, arrayNew, 0, l);
                    arrayNew[l] = p;// tail
                    this.pooledArray = arrayNew;
                    return p;
                } catch (Throwable e) {
                    if (rawObj != null) this.objectFactory.destroy(key, rawObj);
                    throw e instanceof Exception ? (Exception) e : new ObjectCreateException(e);
                }
            }
            return null;
        } finally {
            pooledArrayLock.unlock();
        }
    }

    //Method-2.3: remove one pooled object
    private void removePooledEntry(PooledObject p, String removeType) {
        if (this.printRuntimeLog)
            Log.info("BeeOP({}))begin to remove a pooled object:{},reason:{}", this.poolName, p, removeType);
        p.onBeforeRemove();

        this.pooledArrayLock.lock();
        try {
            for (int l = this.pooledArray.length, i = l - 1; i >= 0; i--) {
                if (this.pooledArray[i] == p) {
                    PooledObject[] arrayNew = new PooledObject[l - 1];
                    System.arraycopy(this.pooledArray, 0, arrayNew, 0, i);
                    int m = l - i - 1;
                    if (m > 0) System.arraycopy(this.pooledArray, i + 1, arrayNew, i, m);
                    this.pooledArray = arrayNew;
                    if (this.printRuntimeLog)
                        Log.info("BeeOP({}))has removed pooled object:{},reason:{}", this.poolName, p, removeType);
                    break;
                }
            }
        } finally {
            pooledArrayLock.unlock();
        }
    }

    //***************************************************************************************************************//
    //                  3: Pooled object borrow and release methods(8)                                               //                                                                                  //
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
            throw new ObjectGetForbiddenException("Access forbidden,generic object pool was closed or in clearing");

        //0:try to get from threadLocal cache
        ObjectBorrower b = this.threadLocal.get().get();
        if (b != null) {
            PooledObject p = b.lastUsed;
            if (p != null && p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING)) {
                if (this.testOnBorrow(p)) return handleFactory.createHandle(p, b);
                b.lastUsed = null;
            }
        } else {
            b = new ObjectBorrower();
            this.threadLocal.set(new WeakReference<ObjectBorrower>(b));
        }

        long deadline = System.nanoTime();
        try {
            //1:try to acquire a permit
            if (!this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ObjectGetTimeoutException("Object get timeout at semaphore");
        } catch (InterruptedException e) {
            throw new ObjectGetInterruptedException("Object get request interrupted at semaphore");
        }

        //2:try search one or create one
        PooledObject p;
        try {//semaphore acquired
            p = this.searchOrCreate();
            if (p != null) {
                semaphore.release();
                return handleFactory.createHandle(p, b);
            }
        } catch (Exception e) {
            semaphore.release();
            throw e;
        }

        //3:try to get one transferred one
        b.state = null;
        this.waitQueue.offer(b);
        BeeObjectException cause = null;
        deadline += this.maxWaitNs;

        do {
            Object s = b.state;
            if (s instanceof PooledObject) {
                p = (PooledObject) s;
                if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                    waitQueue.remove(b);
                    semaphore.release();
                    return handleFactory.createHandle(p, b);
                }
            } else if (s instanceof Throwable) {
                waitQueue.remove(b);
                semaphore.release();
                throw s instanceof Exception ? (Exception) s : new ObjectGetException((Throwable) s);
            }

            if (cause != null) {
                BorrowStUpd.compareAndSet(b, s, cause);
            } else if (s != null) {//here:s must be a PooledObject
                b.state = null;
            } else {//here:(state == null)
                long t = deadline - System.nanoTime();
                if (t > spinForTimeoutThreshold) {
                    if (this.servantTryCount.get() > 0 && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING)) {
                        parentPool.submitServantTask(this);
                    }

                    LockSupport.parkNanos(t);//park exit:1:get transfer 2:timeout 3:interrupted
                    if (Thread.interrupted())
                        cause = new ObjectGetInterruptedException("Object get request interrupted in wait queue");
                } else if (t <= 0L) {//timeout
                    cause = new ObjectGetTimeoutException("Object get timeout in wait queue");
                }
            }//end (state == BOWER_NORMAL)
        } while (true);//while
    }

    //Method-3.2: search one idle Object,if not found,then try to create one
    private PooledObject searchOrCreate() throws Exception {
        PooledObject[] array = this.pooledArray;
        for (PooledObject p : array) {
            if (p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING) && this.testOnBorrow(p))
                return p;
        }
        if (this.pooledArray.length < this.poolMaxSize)
            return this.createPooledEntry(OBJECT_USING);
        return null;
    }

    //Method-3.3: return object to pool after borrower end of use object
    final void recycle(PooledObject p) {
        if (isCompeteMode) p.state = OBJECT_IDLE;
        Iterator<ObjectBorrower> iterator = waitQueue.iterator();

        while (iterator.hasNext()) {
            ObjectBorrower b = iterator.next();
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
     * Method-3.4: when object create failed,creator thread will transfer caused exception to one waiting borrower,
     * which will exit wait and throw this exception.
     *
     * @param e: transfer Exception to waiter
     */
    private void transferException(Throwable e) {
        Iterator<ObjectBorrower> iterator = waitQueue.iterator();

        while (iterator.hasNext()) {
            ObjectBorrower b = iterator.next();
            if (b.state == null && BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    //Method-3.5: remove object when exception occur in return
    final void abandonOnReturn(PooledObject p) {
        this.removePooledEntry(p, DESC_RM_BAD);
        this.tryWakeupServantThread();
    }

    //Method-3.6: check object alive state,if not alive then remove it from pool
    private boolean testOnBorrow(PooledObject p) {
        if (System.currentTimeMillis() - p.lastAccessTime > this.validAssumeTime && !this.objectFactory.isValid(key, p.raw, this.validTestTimeout)) {
            this.removePooledEntry(p, DESC_RM_BAD);
            this.tryWakeupServantThread();
            return false;
        } else {
            return true;
        }
    }

    //***************************************************************************************************************//
    //                          4: Async Servant(2)                                                                  //                                                                                  //
    //***************************************************************************************************************//
    //Method-4.1: try to wakeup servant thread to work if it waiting
    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount.get();
            if (c >= this.poolMaxSize) return;
        } while (!this.servantTryCount.compareAndSet(c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING)) {
            parentPool.submitServantTask(this);
        }
    }

    //Method-4.2: servant method driven by executor in key pool
    public void run() {
        while (servantState.get() == THREAD_WORKING) {
            int c = servantTryCount.get();
            if (c <= 0 || (waitQueue.isEmpty() && servantTryCount.compareAndSet(c, 0))) break;
            servantTryCount.decrementAndGet();

            try {
                PooledObject p = searchOrCreate();
                if (p != null) recycle(p);
            } catch (Throwable e) {
                this.transferException(e);
            }
        }
        servantState.compareAndSet(THREAD_WORKING, THREAD_WAITING);
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

        //step2: remove idle timeout and hold timeout
        PooledObject[] array = this.pooledArray;
        for (PooledObject p : array) {
            int state = p.state;
            if (state == OBJECT_IDLE && this.semaphore.availablePermits() == this.semaphoreSize) {//no borrowers on semaphore
                boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime - this.idleTimeoutMs >= 0L;
                if (isTimeoutInIdle && ObjStUpd.compareAndSet(p, state, OBJECT_CLOSED)) {//need close idle
                    this.removePooledEntry(p, DESC_RM_IDLE);
                    this.tryWakeupServantThread();
                }
            } else if (state == OBJECT_USING) {
                if (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L) {//hold timeout
                    BeeObjectHandle handleInUsing = p.handleInUsing;
                    if (handleInUsing != null) {
                        tryCloseObjectHandle(handleInUsing);
                    } else {
                        this.removePooledEntry(p, DESC_RM_BAD);
                        this.tryWakeupServantThread();
                    }
                }
            } else if (state == OBJECT_CLOSED) {
                this.removePooledEntry(p, DESC_RM_CLOSED);
                this.tryWakeupServantThread();
            }
        }

        //step3: print pool info after idle clean
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
        this.semaphore.interruptWaitingThreads();
        PoolInClearingException clearException = new PoolInClearingException("Object pool was in clearing");
        while (!this.waitQueue.isEmpty()) this.transferException(clearException);

        while (true) {
            PooledObject[] array = this.pooledArray;
            for (PooledObject p : array) {
                final int state = p.state;
                if (state == OBJECT_IDLE) {
                    if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_CLOSED))
                        this.removePooledEntry(p, removeReason);
                } else if (state == OBJECT_USING) {
                    BeeObjectHandle handleInUsing = p.handleInUsing;
                    if (handleInUsing != null) {
                        if (forceCloseUsing || (supportHoldTimeout && System.currentTimeMillis() - p.lastAccessTime - holdTimeoutMs >= 0L)) {
                            tryCloseObjectHandle(handleInUsing);
                            if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_CLOSED))
                                this.removePooledEntry(p, removeReason);
                        }
                    } else {
                        this.removePooledEntry(p, removeReason);
                    }
                } else if (state == OBJECT_CLOSED) {
                    this.removePooledEntry(p, removeReason);
                }
            } // for

            if (this.pooledArray.length == 0) break;
            LockSupport.parkNanos(this.delayTimeForNextClearNs);
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
            if ((poolStateCode == POOL_NEW || poolStateCode == POOL_READY) && PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSED)) {
                Log.info("BeeOP({})begin to shutdown", this.poolName);
                this.clear(forceCloseUsing, DESC_RM_DESTROY);
                Log.info("BeeOP({})has shutdown", this.poolName);
                break;
            } else if (poolStateCode == POOL_CLOSED) {
                break;
            } else {
                LockSupport.parkNanos(this.delayTimeForNextClearNs);// default wait 3 seconds
            }
        } while (true);
    }

    //***************************************************************************************************************//
    //                                       8: Pool monitor(9)                                                      //                                                                                  //
    //***************************************************************************************************************//
    String getPoolHostIP() {
        return poolHostIP;
    }

    long getPoolThreadId() {
        return poolThreadId;
    }

    String getPoolThreadName() {
        return poolThreadName;
    }

    String getPoolMode() {
        return poolMode;
    }

    void setPrintRuntimeLog(boolean indicator) {
        printRuntimeLog = indicator;
    }

    private int getTotalSize() {
        return this.pooledArray.length;
    }

    private int getIdleSize() {
        int idleSize = 0;
        PooledObject[] array = this.pooledArray;
        for (PooledObject p : array)
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
        int totSize = this.getTotalSize();
        int idleSize = this.getIdleSize();
        monitorVo.setPoolState(poolState);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(totSize - idleSize);
        monitorVo.setSemaphoreWaitingSize(this.semaphore.getQueueLength());
        monitorVo.setTransferWaitingSize(getTransferWaitingSize());
        return this.monitorVo;
    }

    //***************************************************************************************************************//
    //                                       9: Inner Classes(7)                                                     //                                                                                  //
    //***************************************************************************************************************//
    private static class ObjectHandleFactory {
        BeeObjectHandle createHandle(PooledObject p, ObjectBorrower b) {
            b.lastUsed = p;
            return new ObjectBaseHandle(p);
        }
    }

    private static class ObjectReflectHandleFactory extends ObjectHandleFactory {
        BeeObjectHandle createHandle(PooledObject p, ObjectBorrower b) {
            b.lastUsed = p;
            return new ObjectReflectHandle(p);
        }
    }

    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<ObjectBorrower>> {
        protected WeakReference<ObjectBorrower> initialValue() {
            return new WeakReference<ObjectBorrower>(new ObjectBorrower());
        }
    }

    private static final class FairTransferPolicy implements ObjectTransferPolicy {
        public final int getStateCodeOnRelease() {
            return OBJECT_USING;
        }

        public final boolean tryCatch(PooledObject p) {
            return p.state == OBJECT_USING;
        }
    }

    private static final class CompeteTransferPolicy implements ObjectTransferPolicy {
        public final int getStateCodeOnRelease() {
            return OBJECT_IDLE;
        }

        public final boolean tryCatch(PooledObject p) {
            return ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING);
        }
    }

    private static class PoolSemaphore extends Semaphore {
        PoolSemaphore(int permits, boolean fair) {
            super(permits, fair);
        }

        void interruptWaitingThreads() {
            for (Thread thread : getQueuedThreads()) {
                //Thread.State state = thread.getState();
                //if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                thread.interrupt();//maybe this operation before parking in semaphore
                //}
            }
        }
    }

    private static final class PoolInitAsynCreateThread extends Thread {
        private final int initialSize;
        private final ObjectGenericPool pool;

        PoolInitAsynCreateThread(int initialSize, ObjectGenericPool pool) {
            this.initialSize = initialSize;
            this.pool = pool;
        }

        public void run() {
            try {
                pool.createInitObjects(initialSize, false);
                pool.servantTryCount.set(pool.pooledArray.length);
                if (!pool.waitQueue.isEmpty() && pool.servantState.get() == THREAD_WAITING && pool.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING)) {
                    pool.parentPool.submitServantTask(pool);
                }
            } catch (Throwable e) {
                //do nothing
            }
        }
    }
}
