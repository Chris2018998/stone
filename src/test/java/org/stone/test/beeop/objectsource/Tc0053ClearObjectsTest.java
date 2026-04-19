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
import org.stone.beeop.*;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.beeop.exception.BeePooledObjectKeyNotFoundException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0053ClearObjectsTest {

    @Test
    public void testClearObjectWithDefaultKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        BeeObjectFactory<String, Book> factory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectKeyMonitorVo defaultKeyMonitor = os.getKeyMonitorVo(factory.getDefaultKey());
            Assertions.assertEquals(1, defaultKeyMonitor.getIdleSize());
            Assertions.assertEquals(0, defaultKeyMonitor.getBorrowedSize());

            //clear1
            os.clearKeyObjects(factory.getDefaultKey());
            defaultKeyMonitor = os.getKeyMonitorVo(factory.getDefaultKey());
            Assertions.assertEquals(0, defaultKeyMonitor.getIdleSize());
            Assertions.assertEquals(0, defaultKeyMonitor.getBorrowedSize());

            //add new object
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(factory.getDefaultKey())) {
                Assertions.assertEquals(1, os.getKeyMonitorVo(factory.getDefaultKey()).getBorrowedSize());
            }
            Assertions.assertEquals(1, os.getKeyMonitorVo(factory.getDefaultKey()).getIdleSize());
            Assertions.assertEquals(0, os.getKeyMonitorVo(factory.getDefaultKey()).getBorrowedSize());

            //clear2
            os.clearKeyObjects(factory.getDefaultKey(), false);
            defaultKeyMonitor = os.getKeyMonitorVo(factory.getDefaultKey());
            Assertions.assertEquals(0, defaultKeyMonitor.getIdleSize());
            Assertions.assertEquals(0, defaultKeyMonitor.getBorrowedSize());
        }
    }

    @Test
    public void testClearPooledObjects() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setParkTimeForRetry(1L);//import: time control

        String defaultKey = objectFactory.getDefaultKey();
        String key2 = "Thanking in Rust";

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertFalse(os.existsKey(key2));
            try {
                os.clearKeyObjects(key2);//key not found
                Assertions.fail("Failed to run testcase:[testDeletePooledKey)");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyNotFoundException.class, e);
                Assertions.assertTrue(e.getMessage().startsWith("Not found category pool with key"));
            }

            //test os.clearKeyObjects(Key);
            BeeObjectHandle<String, Book> handle1 = os.getObjectHandle();
            BeeObjectHandle<String, Book> handle2 = os.getObjectHandle(key2);
            handle1.close();
            handle2.close();
            BeeObjectKeyMonitorVo Key1MonitorVo = os.getKeyMonitorVo(defaultKey);
            BeeObjectKeyMonitorVo key2MonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(1, Key1MonitorVo.getIdleSize());
            Assertions.assertEquals(1, key2MonitorVo.getIdleSize());

            os.clearKeyObjects(defaultKey);
            os.clearKeyObjects(key2);
            Key1MonitorVo = os.getKeyMonitorVo(defaultKey);
            key2MonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(0, Key1MonitorVo.getIdleSize());
            Assertions.assertEquals(0, key2MonitorVo.getIdleSize());

            //test os.clearKeyObjects(Key,true);
            os.getObjectHandle();
            os.getObjectHandle(key2);
            Key1MonitorVo = os.getKeyMonitorVo(defaultKey);
            key2MonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(1, Key1MonitorVo.getBorrowedSize());
            Assertions.assertEquals(1, key2MonitorVo.getBorrowedSize());

            os.clearKeyObjects(defaultKey, true);
            os.clearKeyObjects(key2, true);
            Key1MonitorVo = os.getKeyMonitorVo(defaultKey);
            key2MonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(0, Key1MonitorVo.getIdleSize());
            Assertions.assertEquals(0, key2MonitorVo.getIdleSize());
            Assertions.assertEquals(0, Key1MonitorVo.getBorrowedSize());
            Assertions.assertEquals(0, key2MonitorVo.getBorrowedSize());

            long concurrentTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            ClearPooledObjectsThread thread1 = new ClearPooledObjectsThread(os, defaultKey, concurrentTime);
            ClearPooledObjectsThread thread2 = new ClearPooledObjectsThread(os, defaultKey, concurrentTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            if (thread1.getFailException() != null) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, thread1.getFailException());
                Assertions.assertTrue(thread1.getFailException().getMessage().contains("pool has been closed or is clearing"));
            }

            if (thread2.getFailException() != null) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, thread2.getFailException());
                Assertions.assertTrue(thread2.getFailException().getMessage().contains("pool has been closed or is clearing"));
            }
        }
    }

    private static class ClearPooledObjectsThread extends Thread {
        private final long concurrentTime;
        private final String key;
        private final BeeObjectSource<String, Book> os;
        private Exception failException;

        public ClearPooledObjectsThread(BeeObjectSource<String, Book> os, String key, long concurrentTime) {
            this.os = os;
            this.key = key;
            this.concurrentTime = concurrentTime;
        }

        public Exception getFailException() {
            return failException;
        }

        public void run() {
            if (concurrentTime > 0L)
                LockSupport.parkNanos(concurrentTime - System.nanoTime());
            try {
                os.clearKeyObjects(key);
            } catch (Exception e) {
                this.failException = e;
            }
        }
    }
}
