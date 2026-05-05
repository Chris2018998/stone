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
import org.stone.beeop.BeeObjectFactory;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0060PooledBucketAdditionTest {

    @Test
    public void testAddNewKeySuccess() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> bookFactory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.bucketSize());
            Assertions.assertTrue(os.existsBucket(bookFactory.getDefaultKey()));

            //1: new key1
            String newKey1 = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey1)) {
                Assertions.assertEquals(newKey1, ignored.getKey());
                Assertions.assertTrue(os.existsBucket(newKey1));
                Assertions.assertEquals(2, os.bucketSize());
            }

            //2: mock two thread to get with same key
            String newKey2 = "Thanking in C++";
            ObjectBorrowThread thread1 = new ObjectBorrowThread(os, newKey2);
            ObjectBorrowThread thread2 = new ObjectBorrowThread(os, newKey2);
            long concurrentTime = TimeUnit.MILLISECONDS.toNanos(500L) + System.nanoTime();
            thread1.setTargetTimeToRun(concurrentTime);
            thread2.setTargetTimeToRun(concurrentTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
            Assertions.assertTrue(os.existsBucket(newKey2));
            Assertions.assertEquals(3, os.bucketSize());

            //3: mock two thread to get with different key
            String newKey3 = "Thanking in AI";
            String newKey4 = "Thanking in Python";
            ObjectBorrowThread thread3 = new ObjectBorrowThread(os, newKey3);
            ObjectBorrowThread thread4 = new ObjectBorrowThread(os, newKey4);
            long concurrentTime2 = TimeUnit.MILLISECONDS.toNanos(500L) + System.nanoTime();
            thread3.setTargetTimeToRun(concurrentTime2);
            thread4.setTargetTimeToRun(concurrentTime2);
            thread3.start();
            thread4.start();
            thread3.join();
            thread4.join();
            Assertions.assertTrue(os.existsBucket(newKey3));
            Assertions.assertTrue(os.existsBucket(newKey4));
            Assertions.assertEquals(5, os.bucketSize());
        }
    }

    @Test
    public void testKeyCapacity() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setMaxKeySize(1);//max key size ==1

        //1: test key size has reach max
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.bucketSize());

            String key2 = "Thanking in C++";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(key2)) {
                Assertions.fail("[Tc0050NewKeyTest.testKeyCapacity]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Bucket size has reach max capacity", e.getMessage());
            }
        }

        //2: mock two threads to add two different keys
        config.setMaxKeySize(2);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.bucketSize());

            long targetTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            BookGetThread thread1 = new BookGetThread(os, "Thinking in C++", targetTime);
            BookGetThread thread2 = new BookGetThread(os, "Thinking in C#", targetTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            if (thread1.getFailException() != null) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, thread1.getFailException());
                Assertions.assertEquals("Bucket size has reach max capacity", thread1.getFailException().getMessage());
            }

            if (thread2.getFailException() != null) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, thread2.getFailException());
                Assertions.assertEquals("Bucket size has reach max capacity", thread2.getFailException().getMessage());
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
