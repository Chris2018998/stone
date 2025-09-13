/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.BeeObjectSourceConfigException;
import org.stone.beeop.pool.KeyedObjectPool;

import static org.stone.test.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0052KeyPoolClearTest {

    @Test
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
            Assertions.assertEquals("Configuration can't be null", e.getMessage());
        }
    }

    @Test
    public void testNoBorrowed() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        Assertions.assertEquals(2, pool.getPoolMonitorVo().getIdleSize());
        pool.clear(false);
        Assertions.assertEquals(0, pool.getPoolMonitorVo().getIdleSize());
    }

    @Test
    public void testExistsBorrowed() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        Assertions.assertEquals(2, pool.getPoolMonitorVo().getIdleSize());
        pool.getObjectHandle();
        Assertions.assertEquals(1, pool.getPoolMonitorVo().getBorrowedSize());

        pool.clear(true);
        Assertions.assertEquals(0, pool.getPoolMonitorVo().getIdleSize());
        Assertions.assertEquals(0, pool.getPoolMonitorVo().getBorrowedSize());
    }


//    public void testCasFail() throws Exception {
//        BeeObjectSourceConfig config = createDefault();
//        KeyedObjectPool pool = new KeyedObjectPool();
//        pool.init(config);
//        long time = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
//        ClearThread thread1 = new ClearThread(pool, time);
//        ClearThread thread2 = new ClearThread(pool, time);
//        thread1.start();
//        thread2.start();
//        thread1.join();
//        thread2.join();
//        String errorMessage = null;
//        if (thread1.getfailureException() != null)
//            errorMessage = thread1.getfailureException().getMessage();
//        if (errorMessage == null && thread2.getfailureException() != null)
//            errorMessage = thread2.getfailureException().getMessage();
//
//        if (errorMessage != null) Assertions.assertEquals("Object Pool has been closed or is being cleared", errorMessage);
//    }
//
//    private static class ClearThread extends Thread {
//        private final KeyedObjectPool pool;
//        private final long timePoint;
//        private Exception failureException;
//
//        public ClearThread(KeyedObjectPool pool, long timePoint) {
//            this.pool = pool;
//            this.timePoint = timePoint;
//        }
//
//        public Exception getfailureException() {
//            return failureException;
//        }
//
//        public void run() {
//            try {
//                LockSupport.parkNanos(timePoint - System.nanoTime());
//                pool.clear(true);
//            } catch (Exception e) {
//                this.failureException = e;
//            }
//        }
//    }
}
