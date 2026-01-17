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

import org.stone.beeop.exception.BeeObjectSourceCreatedException;
import org.stone.beeop.exception.ObjectGetInterruptedException;
import org.stone.beeop.exception.ObjectGetTimeoutException;
import org.stone.beeop.pool.ObjectKeyMonitorVo;
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
    private long maxWaitNanos = 8000L;//default vale equals same item in config
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
            throw new BeeObjectSourceCreatedException(e);
        }
    }

    private void createPool(BeeObjectSource<K, V> os) throws Exception {
        String poolImplementClassName = os.getPoolImplementClassName();
        if (isBlank(poolImplementClassName)) poolImplementClassName = ObjectPool.class.getName();
        os.pool = createClassInstance(poolImplementClassName, BeeObjectPool.class, "pool");
        os.pool.start(os);
        os.poolInitialized = true;
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
                    throw new ObjectGetTimeoutException("Timeout on waiting for pool ready");
            } catch (InterruptedException e) {
                throw new ObjectGetInterruptedException("An interruption occurred while waiting for pool ready");
            }
            readLock.unlock();
        }

        //visible to concurrency borrowers
        if (poolInitializedCause != null) throw poolInitializedCause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                     2: Pooled Keys maintenance(8+0)                                           //
    //***************************************************************************************************************//
    public int keySize() throws Exception {
        return pool.keySize();
    }

    public boolean existsKey(K key) throws Exception {
        return pool.existsKey(key);
    }

    public boolean suspendKey(K key) throws Exception {
        return pool.suspendKey(key);
    }

    public boolean resumeKey(K key) throws Exception {
        return pool.resumeKey(key);
    }

    public void clearKeyObjects(K key) throws Exception {
        pool.clearKeyObjects(key);
    }

    public void clearKeyObjects(K key, boolean forceRecycleBorrowed) throws Exception {
        pool.clearKeyObjects(key, forceRecycleBorrowed);
    }

    public boolean deleteKey(K key) throws Exception {
        return pool.deleteKey(key);
    }

    public boolean deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        return pool.deleteKey(key, forceRecycleBorrowed);
    }

    //***************************************************************************************************************//
    //                                     3: Pool Maintenance(6+0)                                                  //
    //***************************************************************************************************************//
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

    public boolean isClosed() {
        return pool.isClosed();
    }

    public boolean suspend() throws Exception {
        return pool.suspendPool();
    }

    public boolean resume() throws Exception {
        return pool.resumePool();
    }

    public void restart(boolean forceRecycleBorrowed) throws Exception {
        pool.restart(forceRecycleBorrowed);
    }

    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {
        pool.restart(forceRecycleBorrowed, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    //***************************************************************************************************************//
    //                                     4: Pool Log Print(2+0)                                                    //
    //***************************************************************************************************************//
    public void enableLogPrinter(boolean printRuntimeLog) throws Exception {
        pool.enableLogPrinter(printRuntimeLog);
    }

    public void enableLogPrinter(K key, boolean enable) throws Exception {
        pool.enableLogPrinter(key, enable);
    }

    //***************************************************************************************************************//
    //                                     5: Pool Monitoring(2+0)                                                   //
    //***************************************************************************************************************//
    public BeeObjectPoolMonitorVo<K> getPoolMonitorVo(boolean includeKeys) throws Exception {
        if (poolInitialized) {
            return pool.getPoolMonitorVo(includeKeys);
        } else {
            return new ObjectPoolMonitorVo<>(
                    this.getPoolName(),
                    this.isFairMode(),
                    this.isUseThreadLocal(),
                    this.getMaxKeySize(),
                    this.getMaxActive(),
                    this.getSemaphoreSize(),
                    ObjectPoolStatics.POOL_UNCREATED,
                    this.isPrintRuntimeLogs(),
                    this.isEnableLogCache());
        }
    }

    public BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) throws Exception {
        if (poolInitialized) {
            return pool.getKeyMonitorVo(key);
        } else {
            return new ObjectKeyMonitorVo<>(
                    key,
                    ObjectPoolStatics.POOL_UNCREATED,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    this.isPrintRuntimeLogs());
        }
    }

    //***************************************************************************************************************//
    //                                     6: Pool blocking interrupts(2+0)                                          //
    //***************************************************************************************************************//
    public List<Thread> interruptWaitingThreads() throws Exception {
        if (poolInitialized) {
            return pool.interruptWaitingThreads();
        } else {
            return lock.interruptAllThreads();
        }
    }

    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        if (poolInitialized) {
            return pool.interruptWaitingThreads(key);
        } else {
            return lock.interruptAllThreads();
        }
    }

    //***************************************************************************************************************//
    //                                     7: Method execution logs(8+0)                                             //
    //***************************************************************************************************************//
    public void enableLogCache(boolean enable) throws Exception {
        pool.enableLogCache(enable);
    }

    public void changeLogListener(BeeMethodLogListener<K> listener) throws Exception {
        pool.changeLogListener(listener);
    }

    public void clearPoolLogs() throws Exception {
        pool.clearPoolLogs();
    }

    public List<BeeMethodLog<K>> getPoolLogs() throws Exception {
        return pool.getPoolLogs();
    }

    public void clearKeyLogs(K key) throws Exception {
        pool.clearKeyLogs(key);
    }

    public List<BeeMethodLog<K>> getKeyLogs(K key) throws Exception {
        return pool.getKeyLogs(key);
    }

    public void clearKeyedObjectCallLogs(K key) throws Exception {
        pool.clearKeyedObjectCallLogs(key);
    }

    public List<BeeMethodLog<K>> getKeyedObjectCallLogs(K key) throws Exception {
        return pool.getKeyedObjectCallLogs(key);
    }

    //***************************************************************************************************************//
    //                                     8: Override methods of configuration(1+0)                                 //
    //***************************************************************************************************************//
    public void setMaxWait(long maxWait) {
        super.setMaxWait(maxWait);
        this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
    }
}
