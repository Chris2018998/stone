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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.tools.BeanUtil.createClassInstance;

/**
 * Bee object source impl.
 * <p>
 * Email:  Chris2018998@tom.com
 * Project: <a href="https://github.com/Chris2018998/stone">...</a>
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSource extends BeeObjectSourceConfig {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = SECONDS.toNanos(8L);//default vale equals same item in config
    private BeeKeyedObjectPool pool;
    private boolean ready;
    private Exception cause;

    public BeeObjectSource() {
    }

    public BeeObjectSource(BeeObjectSourceConfig config) {
        try {
            config.copyTo(this);
            createPool(this);
            this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createPool(BeeObjectSource os) throws Exception {
        Class<?> poolClass = Class.forName(os.getPoolImplementClassName());
        BeeKeyedObjectPool pool = (BeeKeyedObjectPool) createClassInstance(poolClass, BeeKeyedObjectPool.class, "pool");

        pool.init(os);
        os.pool = pool;
        os.ready = true;
    }

    //***************************************************************************************************************//
    //                                        1: Object Getting(pool lazy creation if null)                          //
    //***************************************************************************************************************//
    public BeeObjectHandle getObjectHandle() throws Exception {
        if (this.ready) return pool.getObjectHandle();
        return createPoolByLock().getObjectHandle();
    }

    public BeeObjectHandle getObjectHandle(Object key) throws Exception {
        if (this.ready) return pool.getObjectHandle(key);
        return createPoolByLock().getObjectHandle(key);
    }

    private BeeKeyedObjectPool createPoolByLock() throws Exception {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!ready) {
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

        //read lock will reach
        if (cause != null) throw cause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                          2: dataSource close(2)                                               //
    //***************************************************************************************************************//
    public void close() {
        if (pool != null) pool.close();
    }

    public boolean isClosed() {
        return pool == null || pool.isClosed();
    }

    //***************************************************************************************************************//
    //                                          3: override configuration(2)                                         //
    //***************************************************************************************************************//
    //override method
    public void setMaxWait(long maxWait) {
        if (maxWait > 0) {
            super.setMaxWait(maxWait);
            this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
        }
    }

    //override method
    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        if (pool == null) {
            super.setPrintRuntimeLog(printRuntimeLog);//as configuration item
        } else {
            pool.setPrintRuntimeLog(printRuntimeLog);//set to pool
        }
    }

    //***************************************************************************************************************//
    //                                          4: operation on existed pool                                         //
    //***************************************************************************************************************//
    public BeeObjectPoolMonitorVo getPoolMonitorVo() throws Exception {
        return getPool().getPoolMonitorVo();
    }

    public void clear(boolean forceCloseUsing) throws Exception {
        getPool().clear(forceCloseUsing);
    }

    public void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {
        if (config == null) throw new BeeObjectSourceConfigException("Pool configuration can't be null");
        getPool().clear(forceCloseUsing, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    //***************************************************************************************************************//
    //                                          5: key operation (pool must already exist)                           //
    //***************************************************************************************************************//
    public void deleteKey(Object key) throws Exception {
        getPool().deleteKey(key);
    }

    public void deleteKey(Object key, boolean forceCloseUsing) throws Exception {
        getPool().deleteKey(key, forceCloseUsing);
    }

    public BeeObjectPoolMonitorVo getMonitorVo(Object key) throws Exception {
        return getPool().getMonitorVo(key);
    }

    public boolean isPrintRuntimeLog(Object key) throws Exception {
        return getPool().isPrintRuntimeLog(key);
    }

    public void setPrintRuntimeLog(Object key, boolean indicator) throws Exception {
        getPool().setPrintRuntimeLog(key, indicator);
    }

    public int getObjectCreatingCount(Object key) throws Exception {
        return getPool().getObjectCreatingCount(key);
    }

    public int getObjectCreatingTimeoutCount(Object key) throws Exception {
        return getPool().getObjectCreatingTimeoutCount(key);
    }

    public Thread[] interruptObjectCreating(Object key, boolean interruptTimeout) throws Exception {
        return getPool().interruptObjectCreating(key, interruptTimeout);
    }

    private BeeKeyedObjectPool getPool() throws Exception {
        if (pool == null) throw new PoolNotCreatedException("Pool not be created");
        return this.pool;
    }
}
