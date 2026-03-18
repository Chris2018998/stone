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
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0041ObjectPooledKeyTest {

    @Test
    public void testNewKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setInitialSize(2);

        String defaultKey = objectFactory.getDefaultKey();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertTrue(os.existsKey(defaultKey));

            String key2 = "Thanking in Rust";
            BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle(key2);
            Assertions.assertNotNull(bookHandle);
            Assertions.assertEquals(2, os.keySize());
            Assertions.assertTrue(os.existsKey(key2));
            BeeObjectKeyMonitorVo keyMonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(key2, keyMonitorVo.getKeyName());
            Assertions.assertEquals(1, keyMonitorVo.getIdleSize());
            Assertions.assertEquals(1, keyMonitorVo.getBorrowedSize());
            bookHandle.close();

            keyMonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(2, keyMonitorVo.getIdleSize());
            Assertions.assertEquals(0, keyMonitorVo.getBorrowedSize());
        }
    }

    @Test
    public void testNewKeyWithLazyPool() throws Exception {
        TextBookFactory objectFactory = new TextBookFactory();
        String defaultKey = objectFactory.getDefaultKey();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setInitialSize(2);
            os.setObjectFactory(objectFactory);

            String key2 = "Thanking in Rust";
            BeeObjectHandle<String, Book> rustBookHandle = os.getObjectHandle(key2);
            Assertions.assertNotNull(rustBookHandle);
            Assertions.assertEquals(2, os.keySize());
            Assertions.assertTrue(os.existsKey(defaultKey));
            Assertions.assertTrue(os.existsKey(key2));
            rustBookHandle.close();

            BeeObjectKeyMonitorVo javaKeyMonitorVo = os.getKeyMonitorVo(defaultKey);
            Assertions.assertEquals(defaultKey, javaKeyMonitorVo.getKeyName());
            Assertions.assertEquals(2, javaKeyMonitorVo.getIdleSize());
            Assertions.assertEquals(0, javaKeyMonitorVo.getBorrowedSize());

            BeeObjectKeyMonitorVo rustKeyMonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertEquals(key2, rustKeyMonitorVo.getKeyName());
            Assertions.assertEquals(2, rustKeyMonitorVo.getIdleSize());
            Assertions.assertEquals(0, rustKeyMonitorVo.getBorrowedSize());
        }
    }

    @Test
    public void testDeletePooledKey() throws Exception {
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
}
