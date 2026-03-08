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
import org.stone.beeop.exception.BeeObjectSourcePoolRestartedFailureException;
import org.stone.beeop.exception.BeePooledObjectCreatedException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0032ObjectSourcePoolRestartTest {

    @Test
    public void testRestart() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setInitialSize(2);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(2, os.getKeyMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
            BeeObjectHandle<String, Book> handle = os.getObjectHandle();
            handle.abort();
            Assertions.assertEquals(1, os.getKeyMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
            os.restart(true);
            Assertions.assertEquals(0, os.getKeyMonitorVo(objectFactory.getDefaultKey()).getIdleSize());

            //2:with new config
            try {
                os.restart(true, null);
            } catch (Exception e) {
                Assertions.assertTrue(e.getMessage().contains("Object source configuration can't be null"));
            }
            BeeObjectSourceConfig<String, Book> config2 = new BeeObjectSourceConfig<>();
            config2.setObjectFactory(new TextBookFactory());
            config2.setInitialSize(3);
            os.restart(true, config2);
            Assertions.assertEquals(3, os.getKeyMonitorVo(objectFactory.getDefaultKey()).getIdleSize());
        }
    }

//    //@Test
//    public void testRestart() throws Exception {
//        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
//        TextBookFactory objectFactory = new TextBookFactory();
//        config.setObjectFactory(objectFactory);
//        config.setParkTimeForRetry(50L);
//        config.setInitialSize(1);
//        String key = objectFactory.getDefaultKey();
//
//        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
//            //restart 1
//            Assertions.assertEquals(1, os.getKeyMonitorVo(key).getIdleSize());
//            os.restart(false);//no borrowed handles
//            Assertions.assertEquals(0, os.getKeyMonitorVo(key).getIdleSize());//restart successful
//
//            BeeObjectHandle<String, Book> handle = os.getObjectHandle();
//            Assertions.assertNotNull(handle);
//            Assertions.assertEquals(1, os.getKeyMonitorVo(key).getBorrowedSize());
//            os.restart(true);//recycle borrowed handles by force
//            Assertions.assertEquals(0, os.getKeyMonitorVo(key).getIdleSize());//no idle
//            Assertions.assertEquals(0, os.getKeyMonitorVo(key).getBorrowedSize());//no using
//            Assertions.assertTrue(handle.isClosed());//closed by force
//
//            //3: restart(wait borrowed handle return to pool)
//            handle = os.getObjectHandle();
//            Assertions.assertNotNull(handle);
//            Assertions.assertEquals(1, os.getKeyMonitorVo(key).getBorrowedSize());
//            RestartThread restartThread = new RestartThread(os, false);
//            restartThread.start();
//            //sleep 100 milliseconds
//            Thread.sleep(100L);
//            Assertions.assertTrue(restartThread.isAlive());
//            Assertions.assertTrue(os.getKeyMonitorVo(key).isRestarting());
//            Assertions.assertEquals("Pool is restarting", os.toString());
//
//            //sleep 100 milliseconds again
//            Thread.sleep(100L);
//            Assertions.assertTrue(restartThread.isAlive());
//            handle.close();//close the borrowed connection
//            restartThread.join();
//            Assertions.assertEquals(0, os.getKeyMonitorVo(key).getIdleSize());//no idle
//            Assertions.assertEquals(0, os.getKeyMonitorVo(key).getBorrowedSize());//no using
//        }
//    }

    @Test
    public void testRestartWithNewConfig() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setParkTimeForRetry(50L);
        config.setInitialSize(1);
        String key = objectFactory.getDefaultKey();
        config.setPoolName("BeeOP1");

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectKeyMonitorVo vo = os.getKeyMonitorVo(key);
            Assertions.assertEquals(1, vo.getIdleSize());
            Assertions.assertEquals(key, vo.getKeyName());
            Assertions.assertEquals(config.getPoolName(), os.getPoolName());

            //new configuration
            BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createDefault();
            config2.setInitialSize(10);
            config2.setMaxActive(10);
            config2.setObjectFactory(objectFactory);
            config2.setPoolName("BeeOP2");

            os.restart(true, config2);
            Assertions.assertEquals(10, os.getKeyMonitorVo(key).getIdleSize());
            Assertions.assertEquals("BeeOP2", os.getPoolMonitorVo(false).getPoolName());//Great! success!

            //mock restart failure
            objectFactory.setException(new SQLException("Network error"));
            BeeObjectSourceConfig<String, Book> config3 = OsConfigFactory.createDefault();
            config3.setObjectFactory(objectFactory);
            config3.setInitialSize(1);
            try {
                Assertions.assertFalse(os.getKeyMonitorVo(key).isRestartFailed());
                os.restart(true, config3);
                Assertions.fail("[testRestartWithNewConfig]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolRestartedFailureException.class, e);
                Assertions.assertInstanceOf(BeePooledObjectCreatedException.class, e.getCause());
                Assertions.assertInstanceOf(SQLException.class, e.getCause().getCause());
                //check monitor vo
                Assertions.assertTrue(os.getPoolMonitorVo(false).isRestartFailed());
                Assertions.assertEquals("Pool has restarted failed", os.toString());

                //^-^: New configuration is coming to save your app
                BeeObjectSourceConfig<String, Book> config4 = OsConfigFactory.createDefault();
                config4.setPoolName("BeeOP4");
                config4.setInitialSize(5);
                config4.setMaxActive(5);

                os.restart(true, config4);
                Assertions.assertEquals(5, os.getKeyMonitorVo(key).getIdleSize());
                Assertions.assertEquals("BeeOP4", os.getPoolMonitorVo(false).getPoolName());//Congratulation! success!
            }
        }
    }

    @Test
    public void testRestartFailure() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            RestartThread thread1 = new RestartThread(os, true);
            RestartThread thread2 = new RestartThread(os, true);
            long concurrentTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            thread1.setConcurrentTime(concurrentTime);
            thread2.setConcurrentTime(concurrentTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
        }
    }

    private static class RestartThread extends Thread {
        private final boolean force;
        private final BeeObjectSource<String, Book> os;
        private long concurrentTime;
        private Exception failException;

        public RestartThread(BeeObjectSource<String, Book> os, boolean force) {
            this.os = os;
            this.force = force;
        }

        public Exception getFailException() {
            return failException;
        }

        public void setConcurrentTime(long concurrentTime) {
            this.concurrentTime = concurrentTime;
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
