/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

import org.stone.beeop.exception.BeeObjectSourceCreationException;
import org.stone.beeop.exception.BeePooledObjectGetInterruptedException;
import org.stone.beeop.exception.BeePooledObjectGetTimeoutException;
import org.stone.beeop.pool.ObjectPool;
import org.stone.beeop.pool.ObjectPoolMonitorVo;
import org.stone.beeop.pool.ObjectPoolStatics;
import org.stone.tools.extension.InterruptableReentrantReadWriteLock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beeop.pool.ObjectPoolStatics.createDummyPoolImpl;
import static org.stone.tools.BeanUtil.createClassInstance;
import static org.stone.tools.CommonUtil.isBlank;

/**
 * Bee object source
 *
 * <p>
 * Email:  Chris2018998@tom.com
 * Project: <a href="https://github.com/Chris2018998/stone">...</a>
 * </p>
 *
 * @param <K> is pooled key type
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSource<K, V> extends BeeObjectSourceConfig<K, V> implements AutoCloseable {
    private final InterruptableReentrantReadWriteLock lock = new InterruptableReentrantReadWriteLock();
    private final InterruptableReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = 8000L;//default value equals same item in config
    private BeeObjectPool<K, V> pool = createDummyPoolImpl(false);
    private boolean poolInitialized;
    private Exception poolInitializedCause;

    //***************************************************************************************************************//
    //                                         0:constructors(2+0)                                                   //
    //***************************************************************************************************************//
    public BeeObjectSource() {//internal pool lazy created when call getObjectHandle method
    }

    public BeeObjectSource(BeeObjectSourceConfig<K, V> config) {
        try {
            config.copyTo(this);
            createPool(this);
            this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeObjectSourceCreationException(e);
        }
    }

    private void createPool(BeeObjectSource<K, V> os) throws Exception {
        String poolImplementClassName = os.getPoolImplementClassName();
        if (isBlank(poolImplementClassName)) poolImplementClassName = ObjectPool.class.getName();

        BeeObjectPool<K, V> pool = createClassInstance(poolImplementClassName, BeeObjectPool.class, "pool");
        pool.start(os);
        this.pool = pool;
        this.poolInitialized = true;
    }

    //***************************************************************************************************************//
    //                                     1: Pooled objects get(2+1)                                                //
    //***************************************************************************************************************//
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolInitialized) return pool.getObjectHandle();
        return createPoolByLock().getObjectHandle();
    }

    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        if (this.poolInitialized) return pool.getObjectHandle(key);
        return createPoolByLock().getObjectHandle(key);
    }

    //pool lazy creation
    private BeeObjectPool<K, V> createPoolByLock() throws Exception {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!poolInitialized) {
                    poolInitializedCause = null;
                    createPool(this);
                }
            } catch (Exception e) {
                poolInitializedCause = e;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            try {
                if (!readLock.tryLock(maxWaitNanos, TimeUnit.NANOSECONDS))
                    throw new BeePooledObjectGetTimeoutException("Timeout on waiting for pool ready");
            } catch (InterruptedException e) {
                throw new BeePooledObjectGetInterruptedException("An interruption occurred while waiting for pool ready");
            }
            readLock.unlock();
        }

        //visible to concurrency borrowers
        if (poolInitializedCause != null) throw poolInitializedCause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                     2: Pool Maintenance(5+0)                                                  //
    //***************************************************************************************************************//
    public boolean suspend() throws Exception {
        return pool.suspend();
    }

    public boolean resume() throws Exception {
        return pool.resume();
    }

    public void restart(boolean forceRecycleBorrowed) throws Exception {
        pool.restart(forceRecycleBorrowed);
    }

    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {
        pool.restart(forceRecycleBorrowed, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    public void close() {
        if (this.poolInitialized) {
            synchronized (this) {
                if (!pool.isClosed()) {
                    try {
                        pool.close();
                    } finally {
                        this.pool = createDummyPoolImpl(true);//Create a dummy pool
                    }
                }
            }
        }
    }

    //***************************************************************************************************************//
    //                                     3: Pooled Keys maintenance(8+0)                                           //
    //***************************************************************************************************************//
    public int bucketSize() throws Exception {
        return pool.bucketSize();
    }

    public boolean existsBucket(K key) throws Exception {
        return pool.existsBucket(key);
    }

    public boolean suspendBucket(K key) throws Exception {
        return pool.suspendBucket(key);
    }

    public boolean resumeBucket(K key) throws Exception {
        return pool.resumeBucket(key);
    }

    public void clearBucketObjects(K key) throws Exception {
        pool.clearBucketObjects(key);
    }

    public void clearBucketObjects(K key, boolean forceRecycleBorrowed) throws Exception {
        pool.clearBucketObjects(key, forceRecycleBorrowed);
    }

    public boolean deleteBucket(K key) throws Exception {
        return pool.deleteBucket(key);
    }

    public boolean deleteBucket(K key, boolean forceRecycleBorrowed) throws Exception {
        return pool.deleteBucket(key, forceRecycleBorrowed);
    }

    //***************************************************************************************************************//
    //                                     4: Pool Log Print(2+0)                                                    //
    //***************************************************************************************************************//
    public void enableLogPrinter(boolean printRuntimeLog) throws Exception {
        pool.enableLogPrinter(printRuntimeLog);
    }

    public void enableBucketLogPrinter(K key, boolean enable) throws Exception {
        pool.enableBucketLogPrinter(key, enable);
    }

    //***************************************************************************************************************//
    //                                     5: Pool Monitoring(3+0)                                                   //
    //***************************************************************************************************************//
    public String toString() {
        return pool.toString();
    }

    public boolean isLazy() {
        return !this.poolInitialized;
    }

    public boolean isClosed() {
        return pool.isClosed();
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo(boolean includeKeys) throws Exception {
        if (poolInitialized) {
            return pool.getPoolMonitorVo(includeKeys);
        } else {
            return new ObjectPoolMonitorVo(
                    this.getPoolName(),
                    ObjectPoolStatics.POOL_LAZY,
                    this.isPrintRuntimeLogs(),
                    this.isEnableLogCache());
        }
    }

    public BeeObjectBucketMonitorVo getBucketMonitorVo(K key) throws Exception {
        return pool.getBucketMonitorVo(key);
    }

    //***************************************************************************************************************//
    //                                     6: Pool blocking interrupts(2+0)                                          //
    //***************************************************************************************************************//
    public List<Thread> interruptWaitingThreadsInBuckets() throws Exception {
        if (this.poolInitialized) {
            return pool.interruptWaitingThreadsInBuckets();
        } else {
            return lock.interruptAllThreads();//maybe block in pool creation or block in default key startup
        }
    }

    public List<Thread> interruptWaitingThreadsInBucket(K key) throws Exception {
        return pool.interruptWaitingThreadsInBucket(key);
    }

    //***************************************************************************************************************//
    //                                     7: Method execution logs(4+0)                                             //
    //***************************************************************************************************************//
    public void enablePoolLogCache(boolean enable) throws Exception {
        pool.enablePoolLogCache(enable);
    }

    public void changePoolLogListener(BeeMethodLogListener<K> listener) throws Exception {
        pool.changePoolLogListener(listener);
    }

    public void clearPoolLogs() throws Exception {
        pool.clearPoolLogs();
    }

    public List<BeeMethodLog<K>> getPoolLogs() throws Exception {
        return pool.getPoolLogs();
    }

    //***************************************************************************************************************//
    //                                     8: Key method logs(6+0)                                                     //
    //***************************************************************************************************************//
    public void enableBucketLogCache(K key, boolean enable) throws Exception {
        pool.enableBucketLogCache(key, enable);
    }

    public void changeBucketLogListener(K key, BeeMethodLogListener<K> listener) throws Exception {
        pool.changeBucketLogListener(key, listener);
    }

    public void clearBucketLogs(K key) throws Exception {
        pool.clearBucketLogs(key);
    }

    public List<BeeMethodLog<K>> getBucketLogs(K key) throws Exception {
        return pool.getBucketLogs(key);
    }

    public void clearBucketObjectLogs(K key) throws Exception {
        pool.clearBucketObjectLogs(key);
    }

    public List<BeeMethodLog<K>> getBucketObjectLogs(K key) throws Exception {
        return pool.getBucketObjectLogs(key);
    }

    //***************************************************************************************************************//
    //                                     9: Override methods of configuration(1+0)                                 //
    //***************************************************************************************************************//
    public void setMaxWait(long maxWait) {
        super.setMaxWait(maxWait);
        this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
    }
}
