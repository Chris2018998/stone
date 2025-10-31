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

import org.stone.beeop.pool.exception.ObjectGetInterruptedException;
import org.stone.beeop.pool.exception.ObjectGetTimeoutException;
import org.stone.beeop.pool.exception.PoolNotCreatedException;
import org.stone.tools.extension.InterruptableReentrantReadWriteLock;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.tools.BeanUtil.createClassInstance;

/**
 * Bee object source wrap a keyed object pool.
 * <p>
 * Email:  Chris2018998@tom.com
 * Project: <a href="https://github.com/Chris2018998/stone">...</a>
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSource<K, V> extends BeeObjectSourceConfig<K, V> implements Closeable {
    private final InterruptableReentrantReadWriteLock lock = new InterruptableReentrantReadWriteLock();
    private final InterruptableReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = 8000L;//default vale equals same item in config
    private BeeKeyedObjectPool<K, V> pool;
    private boolean poolStarted;
    private Exception cause;

    public BeeObjectSource() {
    }

    public BeeObjectSource(BeeObjectSourceConfig<K, V> config) {
        try {
            config.copyTo(this);
            createPool(this);
            this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BeeObjectSourceCreationException(e);
        }
    }

    private void createPool(BeeObjectSource<K, V> os) throws Exception {
        os.pool = (BeeKeyedObjectPool<K, V>) createClassInstance(os.getPoolImplementClassName(), BeeKeyedObjectPool.class, "pool");
        os.pool.start(os);
        os.poolStarted = true;
    }

    //***************************************************************************************************************//
    //                                          1: Close(2)                                                          //
    //***************************************************************************************************************//
    public void close() {
        if (this.poolStarted) this.pool.close();
    }

    public boolean isClosed() {
        return !this.poolStarted || this.pool.isClosed();
    }

    public boolean isReady() {
        return this.poolStarted && this.pool.isReady();
    }

    //***************************************************************************************************************//
    //                                        2: Pooled objects Get                                                  //
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
    //                                        3: clear pool(4)                                                       //
    //***************************************************************************************************************//
    public void restart(K key) throws Exception {
        getPool().reset(key);
    }

    public void restart(K key, boolean forceRecycleBorrowed) throws Exception {
        getPool().reset(key, forceRecycleBorrowed);
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
    //                                        4: keys maintenance(3)                                                 //
    //***************************************************************************************************************//
    public boolean exists(K key) throws Exception {
        return getPool().exists(key);
    }

    public void deleteKey(K key) throws Exception {
        getPool().deleteKey(key);
    }

    public void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        getPool().deleteKey(key, forceRecycleBorrowed);
    }

    //***************************************************************************************************************//
    //                                        5: Interrupt blocking of object instance creation                      //
    //***************************************************************************************************************//
    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        if (pool != null) {
            return pool.interruptWaitingThreads();
        } else {
            return lock.interruptAllThreads();
        }
    }

    private BeeKeyedObjectPool<K, V> getPool() throws Exception {
        if (!this.poolStarted) throw new PoolNotCreatedException("Internal pool was not ready");
        return this.pool;
    }

    //***************************************************************************************************************//
    //                                        6: Pool monitor                                                        //
    //***************************************************************************************************************//
    public BeeObjectPoolMonitorVo getMonitorVo(K key) throws Exception {
        return getPool().getMonitorVo(key);
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo() throws Exception {
        return getPool().getPoolMonitorVo();
    }

    //***************************************************************************************************************//
    //                                        7: Log print                                                           //
    //***************************************************************************************************************//
    public boolean isEnabledLogPrint(K key) throws Exception {
        return getPool().isEnabledLogPrint(key);
    }

    public void enableLogPrint(K key, boolean enable) throws Exception {
        getPool().enableLogPrint(key, enable);
    }

    public void enableLogPrint(boolean printRuntimeLog) throws Exception {
        this.getPool().enableLogPrint(printRuntimeLog);
    }

    //***************************************************************************************************************//
    //                                       8: Override methods                                                     //
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
}
