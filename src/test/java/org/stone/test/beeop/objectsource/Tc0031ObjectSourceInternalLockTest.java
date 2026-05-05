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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceCreationException;
import org.stone.beeop.exception.BeePooledObjectGetInterruptedException;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.pool.BlockingPool_Park;
import org.stone.test.beeop.objects.pool.RuntimeInterruptedException;
import org.stone.tools.exception.BeanException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.stone.test.base.TestUtil.waitUtilWaiting;

/**
 * @author Chris Liao
 */
public class Tc0031ObjectSourceInternalLockTest {
    private static void checkExceptionForTestInterruptPoolCreatorThread(Throwable e) {
        Assertions.assertNotNull(e);
        Assertions.assertInstanceOf(BeanException.class, e);
        Assertions.assertInstanceOf(RuntimeInterruptedException.class, e.getCause());
    }

    @Test
    //Lazy pool
    public void testTimeoutOnReadLock() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            os.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));
            os.setPoolImplementClassName(BlockingPool_Park.class.getName());//blocking in pool constructor

            //1: Pool creation block on thread1
            ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
            firstThread.start();
            if (waitUtilWaiting(firstThread)) {//first thread in blocking
                //2: launch second thread to get object
                ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
                secondThread.start();
                secondThread.join();
                Assertions.assertEquals("Timeout on waiting for pool ready", secondThread.getFailureCause().getMessage());
                List<Thread> interruptedThreadList = os.interruptWaitingThreadsInBuckets();
                Assertions.assertTrue(interruptedThreadList.contains(firstThread));
            }
        }
    }

    @Test
    //Lazy pool
    public void testInterruptPoolCreatorThread() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            os.setMaxWait(TimeUnit.MILLISECONDS.toMillis(Long.MAX_VALUE));//timeout is very large
            os.setPoolImplementClassName(BlockingPool_Park.class.getName());//blocking in pool constructor

            //1: Pool creation block on thread1
            ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
            firstThread.start();

            if (waitUtilWaiting(firstThread)) {//first thread in blocking
                ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
                ObjectBorrowThread thirdThread = new ObjectBorrowThread(os);
                secondThread.start();
                thirdThread.start();
                if (waitUtilWaiting(secondThread) && waitUtilWaiting(thirdThread)) {
                    firstThread.interrupt();//only interrupt creator of pool
                }

                thirdThread.join();//wait state
                secondThread.join();//wait state
                firstThread.join();//wait state

                //check exception in thread1
                checkExceptionForTestInterruptPoolCreatorThread(firstThread.getFailureCause());
                //check exception in secondThread,thirdThread
                checkExceptionForTestInterruptPoolCreatorThread(thirdThread.getFailureCause());
                checkExceptionForTestInterruptPoolCreatorThread(secondThread.getFailureCause());
            }
        }
    }

    @Test
    //Lazy pool
    public void testInterruptPoolWaitThreads() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            os.setMaxWait(TimeUnit.MILLISECONDS.toMillis(Long.MAX_VALUE));//timeout is very large
            os.setPoolImplementClassName(BlockingPool_Park.class.getName());//blocking in pool constructor

            //1: Pool creation block on thread1
            ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
            firstThread.start();

            if (waitUtilWaiting(firstThread)) {//first thread in blocking
                ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
                secondThread.interrupt();//set block flag
                secondThread.start();
                secondThread.join();//wait state
                Assertions.assertInstanceOf(BeePooledObjectGetInterruptedException.class, secondThread.getFailureCause());

                os.interruptWaitingThreadsInBuckets();
                firstThread.join();//wait state
            }
        }
    }

    @Test
    public void testDelayInPoolConstructor() throws Exception {
        PoolCreatorMockBlockThread blockThread = new PoolCreatorMockBlockThread();
        blockThread.start();
        if (waitUtilWaiting(blockThread)) {//block 1 second in pool instance creation
            blockThread.interrupt();
        }
        blockThread.join();
        Assertions.assertInstanceOf(BeeObjectSourceCreationException.class, blockThread.failureCause);
    }

    private static class PoolCreatorMockBlockThread extends Thread {
        private Exception failureCause;

        public void run() {
            BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
            config.setObjectFactory(new TextBookFactory());
            config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
            config.setPoolImplementClassName(BlockingPool_Park.class.getName());

            try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
                Assertions.fail("[Tc0031ObjectSourcePoolLockTest.testDelayInPoolConstructor]test failed");
            } catch (Exception e) {
                failureCause = e;
            }
        }
    }
}
