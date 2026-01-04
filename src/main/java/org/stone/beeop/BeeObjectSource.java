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
import org.stone.beeop.exception.BeeObjectSourcePoolNotInstantiatedException;
import org.stone.beeop.exception.ObjectGetInterruptedException;
import org.stone.beeop.exception.ObjectGetTimeoutException;
import org.stone.beeop.pool.KeyedObjectPool;
import org.stone.tools.extension.InterruptableReentrantReadWriteLock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.tools.BeanUtil.createClassInstance;
import static org.stone.tools.CommonUtil.isBlank;

/**
 * Bee object source.
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
    private BeeKeyedObjectPool<K, V> pool;
    private boolean poolStarted;
    private Exception cause;

    //***************************************************************************************************************//
    //                                         0:constructors(2+0)                                                   //
    //***************************************************************************************************************//
    public BeeObjectSource() {
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
        if (isBlank(poolImplementClassName)) poolImplementClassName = KeyedObjectPool.class.getName();
        os.pool = createClassInstance(poolImplementClassName, BeeKeyedObjectPool.class, "pool");
        os.pool.start(os);
        os.poolStarted = true;
    }

    //***************************************************************************************************************//
    //                                     1: Pooled objects get(2+1)                                                //
    //***************************************************************************************************************//
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        if (this.poolStarted) return pool.getObjectHandle();
        return createPoolByLock().getObjectHandle();
    }

    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        if (this.poolStarted) return pool.getObjectHandle(key);
        return createPoolByLock().getObjectHandle(key);
    }

    private BeeKeyedObjectPool<K, V> createPoolByLock() throws Exception {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!poolStarted) {
                    cause = null;
                    createPool(this);
                }
            } catch (Exception e) {
                cause = e;
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
        if (cause != null) throw cause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                     2: Pooled Keys maintenance(8+0)                                           //
    //***************************************************************************************************************//
    public int keySize() throws Exception {
        return getPool().keySize();
    }

    public boolean exists(K key) throws Exception {
        return getPool().exists(key);
    }

    public boolean suspendKey(K key) throws Exception {
        return getPool().suspendKey(key);
    }

    public boolean resumeKey(K key) throws Exception {
        return getPool().resumeKey(key);
    }

    public void clearObjects(K key) throws Exception {
        getPool().clearObjects(key);
    }

    public void clearObjects(K key, boolean forceRecycleBorrowed) throws Exception {
        getPool().clearObjects(key, forceRecycleBorrowed);
    }

    public void deleteKey(K key) throws Exception {
        getPool().deleteKey(key);
    }

    public void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        getPool().deleteKey(key, forceRecycleBorrowed);
    }

    //***************************************************************************************************************//
    //                                     3: Pool Maintenance(6+0)                                                  //
    //***************************************************************************************************************//
    public void close() {
        if (this.poolStarted) this.pool.close();
    }

    public boolean isClosed() {
        return !this.poolStarted || this.pool.isClosed();
    }

    public boolean suspend() {
        return this.poolStarted && this.pool.suspendPool();
    }

    public boolean resume() {
        return this.poolStarted && this.pool.resumePool();
    }

    public void restart(boolean forceRecycleBorrowed) throws Exception {
        getPool().restart(forceRecycleBorrowed);
    }

    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {
        getPool().restart(forceRecycleBorrowed, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    //***************************************************************************************************************//
    //                                     4: Pool Log Print(2+0)                                                    //
    //***************************************************************************************************************//
    public void enableLogPrint(boolean printRuntimeLog) throws Exception {
        this.getPool().enableLogPrint(printRuntimeLog);
    }

    public void enableLogPrint(K key, boolean enable) throws Exception {
        getPool().enableLogPrint(key, enable);
    }

    //***************************************************************************************************************//
    //                                     5: Pool Monitoring(2+0)                                                   //
    //***************************************************************************************************************//
    public BeeObjectKeyPoolMonitorVo<K> getPoolMonitorVo(boolean keyMonitor) throws Exception {
        return getPool().getPoolMonitorVo(keyMonitor);
    }

    public BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) throws Exception {
        return getPool().getKeyMonitorVo(key);
    }

    //***************************************************************************************************************//
    //                                     6: Pool blocking interrupts(2+0)                                          //
    //***************************************************************************************************************//
    public List<Thread> interruptWaitingThreads() throws Exception {
        if (pool != null) {
            return pool.interruptWaitingThreads();
        } else {
            return lock.interruptAllThreads();
        }
    }

    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        if (pool != null) {
            return pool.interruptWaitingThreads(key);
        } else {
            return lock.interruptAllThreads();
        }
    }

    //***************************************************************************************************************//
    //                                     7: Method execution logs(6+0)                                             //
    //***************************************************************************************************************//

    /**
     * A switch method to enable or disable method log cache
     *
     * @param enable is true that make cache to collect method logs;false that make it to stop work
     */
    public void enableMethodExecutionLogCache(boolean enable) throws Exception {
        this.getPool().enableMethodExecutionLogCache(enable);
    }

    /**
     * Set a new log listener to pool.
     *
     * @param listener to handle method logs
     */
    public void setMethodExecutionListener(BeeMethodExecutionListener<K> listener) {
        if (poolStarted) {
            pool.setMethodExecutionListener(listener);//set to pool
        } else {
            super.setMethodExecutionListener(listener);
        }
    }

    /**
     * Gets logs from pool with specified type.
     *
     * @return a result list
     */
    public List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(int type) throws Exception {
        return this.getPool().getMethodExecutionLogs(type);
    }

    /**
     * Clears logs from pool with specified type.
     *
     * @return a cleared list
     */
    public List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(int type) throws Exception {
        return this.getPool().clearMethodExecutionLogs(type);
    }

    /**
     * Gets logs from pool with specified type.
     *
     * @param key may be mapping to a set of pooled objects
     * @return a result list
     */
    public List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(K key, int type) throws Exception {
        return this.getPool().getMethodExecutionLogs(key, type);
    }

    /**
     * Clears logs from pool with specified type.
     *
     * @param key may be mapping to a set of pooled objects
     * @return a cleared list
     */
    public List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(K key, int type) throws Exception {
        return this.getPool().clearMethodExecutionLogs(key, type);
    }

    //***************************************************************************************************************//
    //                                     8: Override methods of configuration(2+0)                                 //
    //***************************************************************************************************************//
    public void setMaxWait(long maxWait) {
        super.setMaxWait(maxWait);
        this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
    }

    public void setPrintRuntimeLogs(boolean printRuntimeLogs) {
        if (pool == null) {
            super.setPrintRuntimeLogs(printRuntimeLogs);
        } else {
            pool.enableLogPrint(printRuntimeLogs);//set to pool
        }
    }

    //***************************************************************************************************************//
    //                                     9: private methods(1+0)                                                   //
    //***************************************************************************************************************//
    private BeeKeyedObjectPool<K, V> getPool() throws Exception {
        if (!this.poolStarted)
            throw new BeeObjectSourcePoolNotInstantiatedException("Object source pool not instantiated");
        return this.pool;
    }
}
