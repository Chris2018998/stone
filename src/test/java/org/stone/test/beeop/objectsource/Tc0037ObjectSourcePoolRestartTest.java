/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objectsource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectKeyMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.beeop.exception.BeeObjectSourcePoolNotReadyException;
import org.stone.beeop.exception.BeeObjectSourcePoolRestartedFailureException;
import org.stone.beeop.exception.BeePooledObjectCreationException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0037ObjectSourcePoolRestartTest {

    @Test
    public void testRestart() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setInitialSize(1);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.getKeyMonitorVo(objectFactory.getDefaultKey()).getIdleSize());

            //1: restart
            os.restart(false);
            Assertions.assertEquals(0, os.getKeyMonitorVo(objectFactory.getDefaultKey()).getIdleSize());

            //2: restart by force
            BeeObjectHandle<String, Book> handle = os.getObjectHandle();
            os.restart(true);
            Assertions.assertEquals(0, os.getKeyMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
            Assertions.assertTrue(handle.isClosed());
        }
    }

    @Test
    public void testRestartWithNewConfig() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setParkTimeForRetry(50L);//delay time
        config.setInitialSize(2);
        String poolName = "BeeOP1";
        config.setPoolName(poolName);
        String defaultKey = objectFactory.getDefaultKey();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //get pool morning VO


            BeeObjectKeyMonitorVo vo = os.getKeyMonitorVo(defaultKey);
            Assertions.assertEquals(2, vo.getIdleSize());
            Assertions.assertEquals(defaultKey, vo.getKeyName());
            Assertions.assertEquals(config.getPoolName(), os.getPoolName());

            //1：null configuration
            try {
                os.restart(true, null);
                Assertions.fail("[os.restart]test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourceConfigException.class, e);
            }

            //2：check failed on configuration
            try {
                config.setMaxActive(1);
                os.restart(true, config);
                Assertions.fail("[os.restart]test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolRestartedFailureException.class, e);
                BeeObjectHandle<String, Book> handle = os.getObjectHandle();
                Assertions.assertNotNull(handle);
            }

            //3: correct config
            config.setInitialSize(2);
            config.setMaxActive(2);
            config.setPoolName("BeeOP2");
            os.restart(true, config);
            vo = os.getKeyMonitorVo(defaultKey);
            Assertions.assertEquals(2, vo.getIdleSize());
            Assertions.assertEquals(defaultKey, vo.getKeyName());
            Assertions.assertEquals(config.getPoolName(), os.getPoolName());

            //4: restart failure1
            objectFactory.setException(new SQLException("Network error"));
            try {
                os.restart(true, config);
                Assertions.fail("[os.restart]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolRestartedFailureException.class, e);
                Assertions.assertInstanceOf(BeePooledObjectCreationException.class, e.getCause());
                Assertions.assertInstanceOf(SQLException.class, e.getCause().getCause());

                Assertions.assertTrue(os.getPoolMonitorVo(false).isRestartFailed());
                Assertions.assertEquals("Pool has restarted failed", os.toString());
                try {
                    os.getObjectHandle();
                    Assertions.fail("[os.restart]failed");
                } catch (Exception ee) {
                    Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, ee);
                    objectFactory.setException(null);
                    os.restart(true, config);
                    try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                        Assertions.assertNotNull(handle);
                    }
                }
            }
        }
    }

    @Test
    public void testCasFailure() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            long concurrentTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            RestartThread thread1 = new RestartThread(os, true, concurrentTime);
            RestartThread thread2 = new RestartThread(os, true, concurrentTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            if (thread1.getFailException() != null) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolRestartedFailureException.class, thread1.getFailException());
            }

            if (thread2.getFailException() != null) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolRestartedFailureException.class, thread2.getFailException());
            }
        }
    }

    private static class RestartThread extends Thread {
        private final boolean force;
        private final long concurrentTime;
        private final BeeObjectSource<String, Book> os;
        private Exception failException;

        public RestartThread(BeeObjectSource<String, Book> os, boolean force, long concurrentTime) {
            this.os = os;
            this.force = force;
            this.concurrentTime = concurrentTime;
        }

        public Exception getFailException() {
            return failException;
        }

        public void run() {
            if (concurrentTime > 0L)
                LockSupport.parkNanos(concurrentTime - System.nanoTime());
            try {
                os.restart(force);
            } catch (Exception e) {
                this.failException = e;
            }
        }
    }
}
