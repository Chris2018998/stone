/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0043DataSourceCloseTest {

    @Test
    public void testDatasourceClose() throws Exception {
        BeeDataSource ds = new BeeDataSource(createDefault());
        long targetTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);
        DsCloseThread thread1 = new DsCloseThread(ds, targetTime);
        DsCloseThread thread2 = new DsCloseThread(ds, targetTime);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        Assertions.assertTrue(ds.isClosed());
        Assertions.assertEquals("Pool has been closed", ds.toString());
    }

    private static class DsCloseThread extends Thread {
        private BeeDataSource ds;
        private long delayToTime;

        public DsCloseThread(BeeDataSource ds, long delayToTime) {
            this.ds = ds;
            this.delayToTime = delayToTime;
        }

        public void run() {
            LockSupport.parkNanos(System.nanoTime() - delayToTime);
            ds.close();
        }
    }
}
