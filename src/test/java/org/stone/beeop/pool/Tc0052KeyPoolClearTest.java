/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.BeeObjectSourceConfigException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0052KeyPoolClearTest extends TestCase {

    public void testNoBorrowed() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        Assert.assertEquals(2, pool.getPoolMonitorVo().getIdleSize());
        pool.clear(false);
        Assert.assertEquals(0, pool.getPoolMonitorVo().getIdleSize());
    }

    public void testExistsBorrowed() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        Assert.assertEquals(2, pool.getPoolMonitorVo().getIdleSize());
        pool.getObjectHandle();
        Assert.assertEquals(1, pool.getPoolMonitorVo().getBorrowedSize());

        pool.clear(true);
        Assert.assertEquals(0, pool.getPoolMonitorVo().getIdleSize());
        Assert.assertEquals(0, pool.getPoolMonitorVo().getBorrowedSize());
    }

    public void testNullConfig() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        try {
            pool.clear(true, null);
        } catch (BeeObjectSourceConfigException e) {
            Assert.assertEquals("Configuration for pool reinitialization can' be null", e.getMessage());
        }
    }

    public void testCasFail() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);
        long time = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
        ClearThread thread1 = new ClearThread(pool, time);
        ClearThread thread2 = new ClearThread(pool, time);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        String errorMessage = null;
        if (thread1.getfailureException() != null)
            errorMessage = thread1.getfailureException().getMessage();
        if (errorMessage == null && thread2.getfailureException() != null)
            errorMessage = thread2.getfailureException().getMessage();

        if (errorMessage != null) Assert.assertEquals("Object pool was closed or in cleaning", errorMessage);
    }

    private static class ClearThread extends Thread {
        private final KeyedObjectPool pool;
        private final long timePoint;
        private Exception failureException;

        public ClearThread(KeyedObjectPool pool, long timePoint) {
            this.pool = pool;
            this.timePoint = timePoint;
        }

        public Exception getfailureException() {
            return failureException;
        }

        public void run() {
            try {
                LockSupport.parkNanos(timePoint - System.nanoTime());
                pool.clear(true);
            } catch (Exception e) {
                this.failureException = e;
            }
        }
    }
}
