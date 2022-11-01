/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.RawObjectFactory;
import org.stone.beeop.pool.exception.ObjectException;
import org.stone.beeop.pool.exception.PoolClosedException;
import org.stone.beeop.pool.exception.PoolCreateFailedException;
import org.stone.beeop.pool.exception.PoolInternalException;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * Object Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class FastObjectPool extends Thread implements ObjectPoolJmxBean, ObjectPool, ObjectTransferPolicy {
    private static final AtomicIntegerFieldUpdater<PooledObject> ObjStUpd = IntegerFieldUpdaterImpl.newUpdater(PooledObject.class, "state");
    private static final AtomicReferenceFieldUpdater<Borrower, Object> BorrowStUpd = IntegerFieldUpdaterImpl.AtomicReferenceFieldUpdaterImpl.newUpdater(Borrower.class, Object.class, "state");
    private static final AtomicIntegerFieldUpdater<FastObjectPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(FastObjectPool.class, "poolState");
    private static final Logger Log = LoggerFactory.getLogger(FastObjectPool.class);

    private String poolName;
    private String poolMode;
    private String poolHostIP;
    private long poolThreadId;
    private String poolThreadName;
    private int poolMaxSize;
    private volatile int poolState;
    private boolean isFairMode;
    private boolean isCompeteMode;

    private int semaphoreSize;
    private PoolSemaphore semaphore;
    private long maxWaitNs;//nanoseconds
    private long idleTimeoutMs;//milliseconds
    private long holdTimeoutMs;//milliseconds

    private int stateCodeOnRelease;
    private long validAssumeTime;//milliseconds
    private int validTestTimeout;//seconds
    private long delayTimeForNextClearNs;//nanoseconds
    private PooledObject templatePooledObject;
    private ObjectTransferPolicy transferPolicy;
    private ObjectHandleFactory handleFactory;
    private RawObjectFactory objectFactory;
    private ReentrantLock pooledArrayLock;
    private volatile PooledObject[] pooledArray;

    private AtomicInteger servantState;
    private AtomicInteger servantTryCount;
    private AtomicInteger idleScanState;
    private IdleTimeoutScanThread idleScanThread;
    private ConcurrentLinkedQueue<Borrower> waitQueue;
    private ThreadLocal<WeakReference<Borrower>> threadLocal;
    private BeeObjectSourceConfig poolConfig;
    private ObjectPoolMonitorVo monitorVo;
    private ObjectPoolHook exitHook;
    private boolean printRuntimeLog;

    //***************************************************************************************************************//
    //                1: Pool initialize and Pooled object create/remove methods(4)                                  //                                                                                  //
    //***************************************************************************************************************//

    /**
     * Method-1.1: initialize pool with configuration
     *
     * @param config data source configuration
     * @throws Exception check configuration fail or to create initiated object
     */
    public void init(BeeObjectSourceConfig config) throws Exception {
        if (config == null) throw new PoolCreateFailedException("Configuration can't be null");
        if (this.poolState != POOL_NEW) throw new PoolCreateFailedException("Pool has initialized");

        this.poolConfig = config.check();//why need a copy here?
        this.poolName = this.poolConfig.getPoolName();
        Log.info("BeeOP({})starting....", this.poolName);
        this.poolMaxSize = this.poolConfig.getMaxActive();
        this.objectFactory = this.poolConfig.getObjectFactory();
        this.pooledArrayLock = new ReentrantLock();
        this.pooledArray = new PooledObject[0];

        Class[] objectInterfaces = poolConfig.getObjectInterfaces();
        this.templatePooledObject = new PooledObject(this, objectFactory, objectInterfaces, poolConfig.getExcludeMethodNames());
        this.createInitObjects(this.poolConfig.getInitialSize());
        if (objectInterfaces != null && objectInterfaces.length > 0)
            handleFactory = new ObjectHandleWithProxyFactory();
        else
            handleFactory = new ObjectHandleFactory();

        if (this.poolConfig.isFairMode()) {
            poolMode = "fair";
            isFairMode = true;
            this.transferPolicy = new FairTransferPolicy();
        } else {
            poolMode = "compete";
            isCompeteMode = true;
            this.transferPolicy = this;
        }
        this.stateCodeOnRelease = this.transferPolicy.getStateCodeOnRelease();

        this.idleTimeoutMs = this.poolConfig.getIdleTimeout();
        this.holdTimeoutMs = this.poolConfig.getHoldTimeout();
        this.maxWaitNs = TimeUnit.MILLISECONDS.toNanos(this.poolConfig.getMaxWait());
        this.delayTimeForNextClearNs = TimeUnit.MILLISECONDS.toNanos(this.poolConfig.getDelayTimeForNextClear());
        this.validAssumeTime = this.poolConfig.getValidAssumeTime();
        this.validTestTimeout = this.poolConfig.getValidTestTimeout();
        this.printRuntimeLog = this.poolConfig.isPrintRuntimeLog();

        this.semaphoreSize = this.poolConfig.getBorrowSemaphoreSize();
        this.semaphore = new PoolSemaphore(this.semaphoreSize, isFairMode);
        this.waitQueue = new ConcurrentLinkedQueue<Borrower>();
        this.threadLocal = new BorrowerThreadLocal();
        this.servantTryCount = new AtomicInteger(0);
        this.servantState = new AtomicInteger(THREAD_WORKING);
        this.idleScanState = new AtomicInteger(THREAD_WORKING);
        this.idleScanThread = new IdleTimeoutScanThread(this);
        this.monitorVo = this.createPoolMonitorVo();
        this.exitHook = new ObjectPoolHook(this);
        Runtime.getRuntime().addShutdownHook(this.exitHook);
        this.registerJmx();

        setDaemon(true);
        setPriority(3);
        setName("BeeOP(" + poolName + ")" + "-asynAdd");
        start();

        this.idleScanThread.setDaemon(true);
        this.idleScanThread.setPriority(3);
        this.idleScanThread.setName("BeeOP(" + poolName + ")" + "-idleCheck");
        this.idleScanThread.start();

        this.poolState = POOL_READY;
        Log.info("BeeOP({})has startup{mode:{},init size:{},max size:{},semaphore size:{},max wait:{}ms",
                this.poolName,
                poolMode,
                this.pooledArray.length,
                this.poolMaxSize,
                this.semaphoreSize,
                this.poolConfig.getMaxWait());
    }

    /**
     * Method-1.3: create specified size objects to pool,if zero,then try to create one
     *
     * @throws Exception error occurred in creating objects
     */
    private void createInitObjects(int initSize) throws Exception {
        try {
            int size = initSize > 0 ? initSize : 1;
            for (int i = 0; i < size; i++)
                this.createPooledEntry(OBJECT_IDLE);
        } catch (Throwable e) {
            for (PooledObject pooledEntry : this.pooledArray)
                this.removePooledEntry(pooledEntry, DESC_RM_INIT);
            if (initSize > 0) {
                if (e instanceof Exception)
                    throw (Exception) e;
                else
                    throw new Exception(e);
            }
        }
    }

    //Method-1.4:create one pooled object
    private PooledObject createPooledEntry(int state) throws Exception {
        this.pooledArrayLock.lock();
        try {
            int l = this.pooledArray.length;
            if (l < this.poolMaxSize) {
                if (this.printRuntimeLog)
                    Log.info("BeeOP({}))begin to create a new pooled object,state:{}", this.poolName, state);

                Object rawObj = null;
                try {
                    rawObj = this.objectFactory.create();
                    PooledObject p = this.templatePooledObject.setDefaultAndCopy(rawObj, state);
                    if (this.printRuntimeLog)
                        Log.info("BeeOP({}))has created a new pooled object:{},state:{}", this.poolName, p, state);
                    PooledObject[] arrayNew = new PooledObject[l + 1];
                    System.arraycopy(this.pooledArray, 0, arrayNew, 0, l);
                    arrayNew[l] = p;// tail
                    this.pooledArray = arrayNew;
                    return p;
                } catch (Throwable e) {
                    if (rawObj != null) this.objectFactory.destroy(rawObj);
                    throw e instanceof Exception ? (Exception) e : new PoolInternalException(e);
                }
            } else {
                return null;
            }
        } finally {
            pooledArrayLock.unlock();
        }
    }

    //Method-1.5: remove one pooled object
    private void removePooledEntry(PooledObject p, String removeType) {
        if (this.printRuntimeLog)
            Log.info("BeeOP({}))begin to remove pooled object:{},reason:{}", this.poolName, p, removeType);
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
    //                  2: Pooled object borrow and release methods(8)                                               //                                                                                  //
    //***************************************************************************************************************//

    /**
     * Method-2.1:borrow one object from pool,if search one idle object in pool,then try to catch it and return it
     * if not search,then wait until other borrowers release objects or wait timeout
     *
     * @return pooled object,
     * @throws Exception if pool is closed or waiting timeout,then throw exception
     */
    public BeeObjectHandle getObject() throws Exception {
        if (this.poolState != POOL_READY) throw new PoolClosedException("Pool has shut down or in clearing");

        //0:try to get from threadLocal cache
        Borrower b = this.threadLocal.get().get();
        if (b != null) {
            PooledObject p = b.lastUsed;
            if (p != null && p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING)) {
                if (this.testOnBorrow(p)) return handleFactory.createHandle(p, b);
                b.lastUsed = null;
            }
        } else {
            b = new Borrower();
            this.threadLocal.set(new WeakReference<Borrower>(b));
        }

        long deadline = System.nanoTime();
        try {
            //1:try to acquire a synchronizer
            if (!this.semaphore.tryAcquire(this.maxWaitNs, TimeUnit.NANOSECONDS))
                throw new ObjectException("Get object timeout");
        } catch (InterruptedException e) {
            throw new ObjectException("Interrupted during getting object");
        }
        try {//semaphore acquired
            //2:try search one or create one
            PooledObject p = this.searchOrCreate();
            if (p != null) return handleFactory.createHandle(p, b);

            //3:try to get one transferred one
            b.state = null;
            this.waitQueue.offer(b);
            boolean failed = false;
            Throwable cause = null;
            deadline += this.maxWaitNs;

            do {
                Object s = b.state;
                if (s instanceof PooledObject) {
                    p = (PooledObject) s;
                    if (this.transferPolicy.tryCatch(p) && this.testOnBorrow(p)) {
                        this.waitQueue.remove(b);
                        return handleFactory.createHandle(p, b);
                    }
                } else if (s instanceof Throwable) {
                    this.waitQueue.remove(b);
                    throw s instanceof Exception ? (Exception) s : new PoolInternalException((Throwable) s);
                }

                if (failed) {
                    BorrowStUpd.compareAndSet(b, s, cause);
                } else if (s instanceof PooledObject) {
                    b.state = null;
                    Thread.yield();
                } else {//here:(state == null)
                    long t = deadline - System.nanoTime();
                    if (t > 0L) {
                        if (this.servantTryCount.get() > 0 && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
                            LockSupport.unpark(this);

                        LockSupport.parkNanos(t);//block exit:1:get transfer 2:timeout 3:interrupted
                        if (Thread.interrupted()) {
                            failed = true;
                            cause = new ObjectException("Interrupted during getting object");
                            if (b.state == null) BorrowStUpd.compareAndSet(b, null, cause);
                        }
                    } else {//timeout
                        failed = true;
                        cause = new ObjectException("Get object timeout");
                    }
                }//end (state == BOWER_NORMAL)
            } while (true);//while
        } finally {
            this.semaphore.release();
        }
    }

    //Method-2.2: search one idle Object,if not found,then try to create one
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

    //Method-2.3: try to wakeup servant thread to work if it waiting
    private void tryWakeupServantThread() {
        int c;
        do {
            c = this.servantTryCount.get();
            if (c >= this.poolMaxSize) return;
        } while (!this.servantTryCount.compareAndSet(c, c + 1));
        if (!this.waitQueue.isEmpty() && this.servantState.get() == THREAD_WAITING && this.servantState.compareAndSet(THREAD_WAITING, THREAD_WORKING))
            LockSupport.unpark(this);
    }

    //Method-2.4: return object to pool after borrower end of use object
    public final void recycle(PooledObject p) {
        if (isCompeteMode) p.state = OBJECT_IDLE;
        Iterator<Borrower> iterator = waitQueue.iterator();

        while (iterator.hasNext()) {
            Borrower b = iterator.next();
            if (p.state != stateCodeOnRelease) return;
            if (BorrowStUpd.compareAndSet(b, null, p)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }

        if (isFairMode) p.state = OBJECT_IDLE;
        tryWakeupServantThread();
    }


    /**
     * Method-2.5:when object create failed,creator thread will transfer caused exception to one waiting borrower,
     * which will exit wait and throw this exception.
     *
     * @param e: transfer Exception to waiter
     */
    private void transferException(Throwable e) {
        Iterator<Borrower> iterator = waitQueue.iterator();

        while (iterator.hasNext()) {
            Borrower b = iterator.next();
            if (BorrowStUpd.compareAndSet(b, null, e)) {
                LockSupport.unpark(b.thread);
                return;
            }
        }
    }

    //Method-2.6: remove object when exception occur in return
    public void abandonOnReturn(PooledObject p) {
        this.removePooledEntry(p, DESC_RM_BAD);
        this.tryWakeupServantThread();
    }

    //Method-2.7: check object alive state,if not alive then remove it from pool
    private boolean testOnBorrow(PooledObject p) {
        if (System.currentTimeMillis() - p.lastAccessTime > this.validAssumeTime && !this.objectFactory.isValid(p, this.validTestTimeout)) {
            this.removePooledEntry(p, DESC_RM_BAD);
            this.tryWakeupServantThread();
            return false;
        } else {
            return true;
        }
    }

    //Compete Pooled connection transfer
    //private static final class CompeteTransferPolicy implements ObjectTransferPolicy {
    public final int getStateCodeOnRelease() {
        return OBJECT_IDLE;
    }

    public final boolean tryCatch(PooledObject p) {
        return p.state == OBJECT_IDLE && ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_USING);
    }

    //***************************************************************************************************************//
    //               3: Pooled object idle-timeout/hold-timeout scan methods(4)                                      //                                                                                  //
    //***************************************************************************************************************//
    //Method-3.1: check whether exists borrows under semaphore
    private boolean existBorrower() {
        return this.semaphoreSize > this.semaphore.availablePermits();
    }

    //Method-3.2 shutdown two work threads in pool
    private void shutdownPoolThread() {
        int curState = this.servantState.get();
        this.servantState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this);

        curState = this.idleScanState.get();
        this.idleScanState.set(THREAD_EXIT);
        if (curState == THREAD_WAITING) LockSupport.unpark(this.idleScanThread);
    }

    //Method-3.3: pool servant thread run method
    public void run() {
        while (poolState != POOL_CLOSED) {
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

            if (servantState.get() == THREAD_EXIT)
                break;
            if (servantTryCount.get() == 0 && servantState.compareAndSet(THREAD_WORKING, THREAD_WAITING))
                LockSupport.park();
        }
    }

    /**
     * Method-3.4: inner timer will call the method to clear some idle timeout objects
     * or dead objects,or long parkTime not active objects in using state
     */
    private void closeIdleTimeoutPooledEntry() {
        if (this.poolState == POOL_READY) {
            PooledObject[] array = this.pooledArray;
            for (PooledObject p : array) {
                int state = p.state;
                if (state == OBJECT_IDLE && !this.existBorrower()) {
                    boolean isTimeoutInIdle = System.currentTimeMillis() - p.lastAccessTime - this.idleTimeoutMs >= 0L;
                    if (isTimeoutInIdle && ObjStUpd.compareAndSet(p, state, OBJECT_CLOSED)) {//need close idle
                        this.removePooledEntry(p, DESC_RM_IDLE);
                        this.tryWakeupServantThread();
                    }
                } else if (state == OBJECT_USING) {
                    if (System.currentTimeMillis() - p.lastAccessTime - this.holdTimeoutMs >= 0L) {//hold timeout
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

            if (this.printRuntimeLog) {
                ObjectPoolMonitorVo vo = getPoolMonitorVo();
                Log.info("BeeOP({})idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
            }
        }
    }

    //***************************************************************************************************************//
    //                                      4: Pool clear/close methods(5)                                           //                                                                                  //
    //***************************************************************************************************************//
    //Method-4.1: remove all connections from pool
    public void clear() {
        this.clear(false);
    }

    //Method-4.2: remove all connections from pool
    public void clear(boolean force) {
        if (PoolStateUpd.compareAndSet(this, POOL_READY, POOL_CLEARING)) {
            Log.info("BeeOP({})begin to remove objects", this.poolName);
            this.clear(force, DESC_RM_CLEAR);
            this.poolState = POOL_READY;// restore state;
            Log.info("BeeOP({})all objects were removed and restored to accept new requests", this.poolName);
        }
    }

    //Method-4.3: remove all connections from pool
    private void clear(boolean force, String source) {
        this.semaphore.interruptWaitingThreads();
        PoolClosedException poolCloseException = new PoolClosedException("Pool has shut down or in clearing");
        while (!this.waitQueue.isEmpty()) this.transferException(poolCloseException);

        while (this.pooledArray.length > 0) {
            PooledObject[] array = this.pooledArray;
            for (PooledObject p : array) {
                final int state = p.state;
                if (state == OBJECT_IDLE) {
                    if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_CLOSED))
                        this.removePooledEntry(p, source);
                } else if (state == OBJECT_USING) {
                    BeeObjectHandle handleInUsing = p.handleInUsing;
                    if (handleInUsing != null) {
                        if (force || System.currentTimeMillis() - p.lastAccessTime - this.holdTimeoutMs >= 0L) {
                            tryCloseObjectHandle(handleInUsing);
                            if (ObjStUpd.compareAndSet(p, OBJECT_IDLE, OBJECT_CLOSED))
                                this.removePooledEntry(p, source);
                        }
                    } else {
                        this.removePooledEntry(p, source);
                    }
                } else if (state == OBJECT_CLOSED) {
                    this.removePooledEntry(p, source);
                }
            } // for
            if (this.pooledArray.length > 0) LockSupport.parkNanos(this.delayTimeForNextClearNs);
        } // while

        if (this.printRuntimeLog) {
            ObjectPoolMonitorVo vo = getPoolMonitorVo();
            Log.info("BeeOP({})idle:{},using:{},semaphore-waiting:{},transfer-waiting:{}", this.poolName, vo.getIdleSize(), vo.getUsingSize(), vo.getSemaphoreWaitingSize(), vo.getTransferWaitingSize());
        }
    }

    //Method-4.4: closed check
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    // Method-4.5: close pool
    public void close() {
        do {
            int poolStateCode = this.poolState;
            if ((poolStateCode == POOL_NEW || poolStateCode == POOL_READY) && PoolStateUpd.compareAndSet(this, poolStateCode, POOL_CLOSED)) {
                Log.info("BeeOP({})begin to shutdown", this.poolName);
                this.clear(this.poolConfig.isForceCloseUsingOnClear(), DESC_RM_DESTROY);
                this.unregisterJmx();
                this.shutdownPoolThread();

                try {
                    Runtime.getRuntime().removeShutdownHook(this.exitHook);
                } catch (Throwable e) {
                    //do nothing
                }
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
    //                                       5: Pool monitor/jmx methods(12)                                         //                                                                                  //
    //***************************************************************************************************************//
    //Method-5.1: set pool info debug switch
    public void setPrintRuntimeLog(boolean indicator) {
        printRuntimeLog = indicator;
    }

    //Method-5.2: size of all pooled object
    public int getTotalSize() {
        return this.pooledArray.length;
    }

    //Method-5.3: size of idle pooled object
    public int getIdleSize() {
        int idleSize = 0;
        PooledObject[] array = this.pooledArray;
        for (PooledObject p : array)
            if (p.state == OBJECT_IDLE) idleSize++;
        return idleSize;
    }

    //Method-5.4: size of using pooled connections
    public int getUsingSize() {
        int active = this.pooledArray.length - this.getIdleSize();
        return (active > 0) ? active : 0;
    }

    //Method-5.5: waiting size for semaphore
    public int getSemaphoreAcquiredSize() {
        return this.poolConfig.getBorrowSemaphoreSize() - this.semaphore.availablePermits();
    }

    //Method-5.6: using size of semaphore synchronizer
    public int getSemaphoreWaitingSize() {
        return this.semaphore.getQueueLength();
    }

    //Method-5.7: waiting size in transfer queue
    public int getTransferWaitingSize() {
        int size = 0;
        for (Borrower borrower : this.waitQueue) {
            if (borrower.state == null) size++;
        }
        return size;
    }

    //Method-5.8: register pool to jmx
    private void registerJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.registerJmxBean(mBeanServer, String.format(" FastObjectPool:type=BeeOP(%s)", this.poolName), this);
            this.registerJmxBean(mBeanServer, String.format("BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolName), this.poolConfig);
        }
    }

    //Method-5.9: jmx register
    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (!mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.registerMBean(bean, jmxRegName);
            }
        } catch (Exception e) {
            Log.warn("BeeOP({})failed to register jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.10: pool unregister from jmx
    private void unregisterJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.unregisterJmxBean(mBeanServer, String.format(" FastObjectPool:type=BeeOP(%s)", this.poolName));
            this.unregisterJmxBean(mBeanServer, String.format("BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolName));
        }
    }

    //Method-5.11: jmx unregister
    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.unregisterMBean(jmxRegName);
            }
        } catch (Exception e) {
            Log.warn("BeeOP({})failed to unregister jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.12 create monitor vo
    private ObjectPoolMonitorVo createPoolMonitorVo() {
        Thread currentThread = Thread.currentThread();
        this.poolThreadId = currentThread.getId();
        this.poolThreadName = currentThread.getName();

        try {
            this.poolHostIP = (InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            Log.info("BeeOP({})failed to resolve pool hose ip", this.poolName);
        }
        return new ObjectPoolMonitorVo();
    }

    //Method-5.12: pool monitor vo
    public ObjectPoolMonitorVo getPoolMonitorVo() {
        monitorVo.setPoolName(poolName);
        monitorVo.setPoolMode(poolMode);
        monitorVo.setPoolMaxSize(poolMaxSize);
        monitorVo.setThreadId(poolThreadId);
        monitorVo.setThreadName(poolThreadName);
        monitorVo.setHostIP(poolHostIP);

        int totSize = this.getTotalSize();
        int idleSize = this.getIdleSize();
        monitorVo.setPoolState(poolState);
        monitorVo.setIdleSize(idleSize);
        monitorVo.setUsingSize(totSize - idleSize);
        monitorVo.setSemaphoreWaitingSize(this.getSemaphoreWaitingSize());
        monitorVo.setTransferWaitingSize(this.getTransferWaitingSize());
        return this.monitorVo;
    }

    //***************************************************************************************************************//
    //                                  6: Pool inner interface/class(4)                                             //                                                                                  //
    //***************************************************************************************************************//

    //class-6.1:Compete Pooled connection transfer
    private static final class FairTransferPolicy implements ObjectTransferPolicy {
        public final int getStateCodeOnRelease() {
            return OBJECT_USING;
        }

        public final boolean tryCatch(PooledObject p) {
            return p.state == OBJECT_USING;
        }
    }

    //class-6.2:Semaphore extend
    private static final class PoolSemaphore extends Semaphore {
        PoolSemaphore(int permits, boolean fair) {
            super(permits, fair);
        }

        void interruptWaitingThreads() {
            for (Thread thread : getQueuedThreads()) {
                Thread.State state = thread.getState();
                if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                    thread.interrupt();
                }
            }
        }
    }

    //class-6.3:Idle scan thread
    private static final class IdleTimeoutScanThread extends Thread {
        private final FastObjectPool pool;
        private final AtomicInteger idleScanState;

        IdleTimeoutScanThread(FastObjectPool pool) {
            this.pool = pool;
            idleScanState = pool.idleScanState;
        }

        public void run() {
            long checkTimeIntervalNanos = TimeUnit.MILLISECONDS.toNanos(this.pool.poolConfig.getTimerCheckInterval());
            while (this.idleScanState.get() == THREAD_WORKING) {
                LockSupport.parkNanos(checkTimeIntervalNanos);
                try {
                    this.pool.closeIdleTimeoutPooledEntry();
                } catch (Throwable e) {
                    //do nothing
                }
            }
        }
    }

    //class-6.4: handle factory
    private static class ObjectHandleFactory {
        BeeObjectHandle createHandle(PooledObject p, Borrower b) {
            b.lastUsed = p;
            return new ObjectBaseHandle(p);
        }
    }

    //class-6.5: supported proxy handle factory
    private static class ObjectHandleWithProxyFactory extends ObjectHandleFactory {
        BeeObjectHandle createHandle(PooledObject p, Borrower b) {
            b.lastUsed = p;
            return new ObjectProxyHandle(p);
        }
    }

    //class-6.6:Borrower ThreadLocal
    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<Borrower>> {
        protected WeakReference<Borrower> initialValue() {
            return new WeakReference<Borrower>(new Borrower());
        }
    }

    //class-6.7:JVM exit hook
    private static class ObjectPoolHook extends Thread {
        private final FastObjectPool pool;

        ObjectPoolHook(FastObjectPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                Log.info("BeeOP({})PoolHook Running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeOP({})Error at closing pool,cause:", this.pool.poolName, e);
            }
        }
    }
}
