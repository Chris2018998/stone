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

import org.stone.beeop.pool.ObjectPoolStatics;
import org.stone.beeop.pool.exception.ObjectGetInterruptedException;
import org.stone.beeop.pool.exception.ObjectGetTimeoutException;
import org.stone.beeop.pool.exception.PoolNotCreatedException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Bee Object object source
 * <p>
 * Email:  Chris2018998@tom.com
 * Project: https://github.com/Chris2018998/stone
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeObjectSource extends BeeObjectSourceConfig {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = SECONDS.toNanos(8);//default vale equals same item in config
    private BeeKeyedObjectPool pool;
    private boolean ready;
    private Exception cause;

    //***************************************************************************************************************//
    //                                             1:constructors(2)                                                 //
    //***************************************************************************************************************//
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
        BeeKeyedObjectPool pool = (BeeKeyedObjectPool) ObjectPoolStatics.createClassInstance(poolClass, BeeKeyedObjectPool.class, "pool");

        pool.init(os);
        os.pool = pool;
        os.ready = true;
    }

    //***************************************************************************************************************//
    //                                        2: object take methods(3)                                              //
    //***************************************************************************************************************//
    public final BeeObjectHandle getObjectHandle() throws Exception {
        if (this.ready) return pool.getObjectHandle();
        return createPoolByLock().getObjectHandle();
    }

    public final BeeObjectHandle getObjectHandle(Object key) throws Exception {
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
                    throw new ObjectGetTimeoutException("Object get timeout at lock of pool creation");
            } catch (InterruptedException e) {
                throw new ObjectGetInterruptedException("Object get request interrupted at lock of pool creation");
            }
            readLock.unlock();
        }

        //read lock will reach
        if (cause != null) throw cause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                          3: pool other methods(7)                                             //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return pool == null || pool.isClosed();
    }

    public void close() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Throwable e) {
                ObjectPoolStatics.CommonLog.error("Error at closing pool,cause:", e);
            }
        }
    }

    //override method
    public void setMaxWait(long maxWait) {
        if (maxWait > 0) {
            super.setMaxWait(maxWait);
            this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
        }
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        if (pool != null) pool.setPrintRuntimeLog(printRuntimeLog);
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo() throws Exception {
        if (pool == null) throw new PoolNotCreatedException("Object pool not initialized");
        return pool.getPoolMonitorVo();
    }

    public void clear(boolean forceCloseUsing) throws Exception {
        if (pool == null) throw new PoolNotCreatedException("Object pool not initialized");
        pool.clear(forceCloseUsing);
    }

    public void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {
        if (pool == null) throw new PoolNotCreatedException("Object pool not initialized");
        if (config == null) throw new BeeObjectSourceConfigException("Object pool config can't be null");
        pool.clear(forceCloseUsing, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }
}
