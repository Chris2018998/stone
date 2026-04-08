/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.poolkey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectKeyMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeePooledObjectCreationException;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0050AddPooledKeyTest {

    @Test
    public void testAddNewKeySuccess() throws Exception {
        //1: test add new key on initialized key pool
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setMaxKeySize(2);
        config.setInitialSize(1);
        config.setMaxActive(1);
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertTrue(os.existsKey(objectFactory.getDefaultKey()));

            //new key
            String newKey = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle(newKey)) {
                Assertions.assertNotNull(bookHandle);
                Assertions.assertTrue(os.existsKey(newKey));
                Assertions.assertEquals(2, os.keySize());

                BeeObjectKeyMonitorVo keyMonitorVo = os.getKeyMonitorVo(newKey);
                Assertions.assertEquals(newKey, keyMonitorVo.getKeyName());
                Assertions.assertEquals(0, keyMonitorVo.getIdleSize());
                Assertions.assertEquals(1, keyMonitorVo.getBorrowedSize());
            }
        }

        //2: test add new key on lazy key pool
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            Assertions.assertTrue(os.isLazy());
            os.setMaxKeySize(2);
            os.setInitialSize(1);
            os.setMaxActive(1);
            os.setObjectFactory(objectFactory);

            String newKey = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle(newKey)) {
                Assertions.assertNotNull(bookHandle);
                Assertions.assertEquals(2, os.keySize());
                Assertions.assertTrue(os.existsKey(newKey));
                Assertions.assertTrue(os.existsKey(objectFactory.getDefaultKey()));

                BeeObjectKeyMonitorVo keyMonitorVo = os.getKeyMonitorVo(newKey);
                Assertions.assertEquals(newKey, keyMonitorVo.getKeyName());
                Assertions.assertEquals(0, keyMonitorVo.getIdleSize());
                Assertions.assertEquals(1, keyMonitorVo.getBorrowedSize());
            }
        }
    }

    @Test
    public void testKeyInitializeFailure() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setMaxKeySize(2);
        config.setInitialSize(1);
        config.setMaxActive(1);
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertTrue(os.existsKey(objectFactory.getDefaultKey()));

            //new key failed
            String newKey = "Thanking in Rust";
            objectFactory.setException(new Exception("Paper is not enough"));
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey)) {
                Assertions.fail("[Tc0050AddPoolKeyTest.testKeyInitializeFailure]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectCreationException.class, e);
                BeePooledObjectCreationException e1 = (BeePooledObjectCreationException) e;
                Assertions.assertEquals("Paper is not enough", e1.getCause().getMessage());
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
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(key2)) {
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
