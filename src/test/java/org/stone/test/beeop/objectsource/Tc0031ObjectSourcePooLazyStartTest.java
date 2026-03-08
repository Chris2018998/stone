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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.exception.BeeObjectSourcePoolLazyInitializationException;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.pool.BlockingPool_Park;
import org.stone.test.beeop.objects.pool.BlockingPool_ParkNanos;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.base.TestUtil.waitUtilWaiting;

/**
 * @author Chris Liao
 */
public class Tc0031ObjectSourcePooLazyStartTest {
    @Test
    public void testLazyException() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            Assertions.assertTrue(os.isClosed());
            try {
                os.enableLogPrinter(true);
                Assertions.fail("<ObjectSourceLazyTest>Test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolLazyInitializationException.class, e);
            }

            os.setObjectFactory(new TextBookFactory());
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
            Assertions.assertFalse(os.isClosed());
        }
    }

    @Test
    public void testTimeoutOnReadLock() throws Exception {
        BeeObjectSource<String, Book> os = new BeeObjectSource<>();
        os.setObjectFactory(new TextBookFactory());
        os.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));//timeout on wait
        os.setPoolImplementClassName(BlockingPool_Park.class.getName());

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        firstThread.start();

        //1: wait timeout on read lock(secondThread)
        if (waitUtilWaiting(firstThread)) {
            ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
            secondThread.start();
            secondThread.join();
            Assertions.assertEquals("Timeout on waiting for pool ready", secondThread.getFailureCause().getMessage());
        }

        //2: expect to success to get a connection(thirdThread)
        if (waitUtilWaiting(firstThread)) {
            ObjectBorrowThread thirdThread = new ObjectBorrowThread(os);
            LockSupport.unpark(firstThread);//wakeup this first thread
            thirdThread.start();
            thirdThread.join();
            Assertions.assertNotNull(thirdThread.getObjectHandle());

            //3: expect to get a connection
            firstThread.join();
            Assertions.assertNotNull(firstThread.getObjectHandle());
        } else {
            firstThread.interrupt();
            Assertions.fail("[testOnReadLock]Test failed");
        }
    }

    @Test
    public void testInterruptionOnLock() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            os.setMaxWait(Long.MAX_VALUE);//ensure this value enough large
            os.setPoolImplementClassName(BlockingPool_Park.class.getName());

            ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
            firstThread.start();

            if (waitUtilWaiting(firstThread)) {//blocking in pool
                ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
                secondThread.start();
                if (waitUtilWaiting(secondThread)) {//blocking lock
                    List<Thread> threadList = os.interruptWaitingThreads();
                    Assertions.assertTrue(threadList.contains(firstThread));//blocking pool new
                    Assertions.assertTrue(threadList.contains(secondThread));//blocking in ds read-lock
                }

                firstThread.join();
                secondThread.join();
                Assertions.assertEquals("An interruption occurred while waiting for pool ready", secondThread.getFailureCause().getMessage());
            }
        }
    }

    @Test
    public void testDelayInPoolConstructor() throws Exception {
        BeeObjectSource<String, Book> os = new BeeObjectSource<>();
        os.setObjectFactory(new TextBookFactory());
        os.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait
        os.setPoolImplementClassName(BlockingPool_ParkNanos.class.getName());

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        ObjectBorrowThread secondThread = new ObjectBorrowThread(os);

        firstThread.start();
        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            secondThread.start();
            secondThread.join();
            Assertions.assertNull(secondThread.getFailureCause());
            Assertions.assertNotNull(secondThread.getObjectHandle());
        }
    }
}
