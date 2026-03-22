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
import org.stone.beeop.exception.BeeObjectSourcePoolNotReadyException;
import org.stone.beeop.exception.BeePooledObjectCreatedException;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0042ObjectPooledKeyTest {

    @Test
    public void testGetWithDefaultKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setInitialSize(1);
        config.setMaxActive(1);
        String defaultKey = objectFactory.getDefaultKey();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                Assertions.assertNotNull(bookHandle);
            }

            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle(defaultKey)) {
                Assertions.assertNotNull(bookHandle);
            }
        }
    }

    @Test
    public void testAddNewKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setMaxKeySize(3);
        String defaultKey = objectFactory.getDefaultKey();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertTrue(os.existsKey(defaultKey));

            String key2 = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle(key2)) {
                Assertions.assertNotNull(bookHandle);
                Assertions.assertEquals(2, os.keySize());
                Assertions.assertTrue(os.existsKey(key2));
                BeeObjectKeyMonitorVo keyMonitorVo = os.getKeyMonitorVo(key2);
                Assertions.assertEquals(key2, keyMonitorVo.getKeyName());
                Assertions.assertEquals(1, keyMonitorVo.getIdleSize());
                Assertions.assertEquals(1, keyMonitorVo.getBorrowedSize());
            }
            BeeObjectKeyMonitorVo keyMonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(2, keyMonitorVo.getIdleSize());
            Assertions.assertEquals(0, keyMonitorVo.getBorrowedSize());
            Assertions.assertEquals(2, os.keySize());

            //create failed
            String key3 = "Thanking in C++";
            objectFactory.setException(new Exception("Object Create failed"));
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(key3)) {
                //nothing
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectCreatedException.class, e);
                Assertions.assertEquals(2, os.keySize());
            }
        }
    }

    @Test
    public void testKeyCapacity() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setMaxKeySize(1);
        String defaultKey = objectFactory.getDefaultKey();

        //1: test key size has reach max
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertTrue(os.existsKey(defaultKey));

            //Key capacity test(failed)
            String key2 = "Thanking in C++";
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle(key2)) {
                Assertions.fail("Pooled key capacity test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Pooled key size has reach max capacity", e.getMessage());
            }
        }

        //2: test key size has reach max
        config.setMaxKeySize(2);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());

            long targetTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            BookGetThread thread1 = new BookGetThread(os, "Thinking in C++", targetTime);
            BookGetThread thread2 = new BookGetThread(os, "Thinking in C#", targetTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            if (thread1.getFailException() != null) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, thread1.getFailException());
                Assertions.assertEquals("Pooled key size has reach max capacity", thread1.getFailException().getMessage());
            }

            if (thread2.getFailException() != null) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, thread2.getFailException());
                Assertions.assertEquals("Pooled key size has reach max capacity", thread2.getFailException().getMessage());
            }
        }
    }


    @Test
    public void testDeleteKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setParkTimeForRetry(1L);//import: time control

        String defaultKey = objectFactory.getDefaultKey();
        String key2 = "Thanking in Rust";
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertTrue(os.existsKey(defaultKey));

            //1: try to delete default key
            try {
                os.deleteKey(defaultKey);
                Assertions.fail("Failed to run testcase:[testDeletePooledKey)");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Default key is forbidden to delete", e.getMessage());
            }

            //delete key
            Assertions.assertFalse(os.existsKey(key2));
            Assertions.assertFalse(os.deleteKey(key2));

            //2: add a new pooled key
            BeeObjectHandle<String, Book> rustBookHandle = os.getObjectHandle(key2);
            rustBookHandle.close();
            Assertions.assertEquals(2, os.keySize());
            Assertions.assertTrue(os.existsKey(key2));
            Assertions.assertTrue(os.deleteKey(key2));
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertFalse(os.existsKey(key2));

            //3: test delete key by force
            os.getObjectHandle(key2);
            Assertions.assertTrue(os.existsKey(key2));
            BeeObjectKeyMonitorVo rustKeyMonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertTrue(rustKeyMonitorVo.isReady());
            Assertions.assertEquals(0, rustKeyMonitorVo.getIdleSize());
            Assertions.assertEquals(1, rustKeyMonitorVo.getBorrowedSize());
            Assertions.assertTrue(os.deleteKey(key2, true));
            Assertions.assertFalse(os.existsKey(key2));
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
        }
    }

    @Test
    public void testSuspendKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setParkTimeForRetry(1L);//import: time control

        String defaultKey = objectFactory.getDefaultKey();
        String key2 = "Thanking in Rust";

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectHandle<String, Book> handle1 = os.getObjectHandle();
            BeeObjectHandle<String, Book> handle2 = os.getObjectHandle(key2);
            handle1.close();
            handle2.close();
            Assertions.assertTrue(os.suspendKey(defaultKey));
            Assertions.assertTrue(os.suspendKey(key2));

            try {
                os.getObjectHandle();
                Assertions.fail("testSuspendKey test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, e);
            }
            Assertions.assertTrue(os.getKeyMonitorVo(defaultKey).isSuspended());

            try {
                os.getObjectHandle(key2);
                Assertions.fail("testSuspendKey test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, e);
            }
            Assertions.assertTrue(os.getKeyMonitorVo(key2).isSuspended());

            Assertions.assertTrue(os.resumeKey(defaultKey));
            Assertions.assertTrue(os.resumeKey(key2));
            handle1 = os.getObjectHandle();
            Assertions.assertNotNull(handle1);
            handle2 = os.getObjectHandle(key2);
            Assertions.assertNotNull(handle2);
            handle1.close();
            handle2.close();
        }
    }

    private static class BookGetThread extends Thread {
        private final String key;
        private final long concurrentTime;
        private final BeeObjectSource<String, Book> os;
        private Exception failException;

        public BookGetThread(BeeObjectSource<String, Book> os, String key, long concurrentTime) {
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

            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(key)) {
                //nothing
            } catch (Exception e) {
                this.failException = e;
            }
        }
    }
}
