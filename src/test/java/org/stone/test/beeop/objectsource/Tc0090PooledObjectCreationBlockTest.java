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
import org.stone.beeop.BeeObjectBucketMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.List;

import static org.stone.test.base.TestUtil.waitUtilWaiting;

/**
 * @author Chris Liao
 */
public class Tc0090PooledObjectCreationBlockTest {

    @Test
    public void testInterruptBlockInFactory() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setMaxWait(100L);//1 millisecond

        config.setIntervalOfClearTimeout(Long.MAX_VALUE);
        TextBookFactory bookFactory = new TextBookFactory();
        config.setObjectFactory(bookFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(os.existsBucket(bookFactory.getDefaultKey()));

            //interruptWaitingThreads(key)
            bookFactory.addFactoryMethodPauseTime("create", Long.valueOf(Long.MAX_VALUE));

            String newKey = "Thinking in C++";
            ObjectBorrowThread firstThread = new ObjectBorrowThread(os, newKey);
            firstThread.start();
            if (waitUtilWaiting(firstThread)) {
                Assertions.assertEquals(1, os.getBucketMonitorVo(newKey).getCreatingSize());
                Assertions.assertEquals(0, os.getBucketMonitorVo(newKey).getCreatingTimeoutSize());
                Thread.sleep(200L);
                Assertions.assertEquals(1, os.getBucketMonitorVo(newKey).getCreatingSize());
                Assertions.assertEquals(1, os.getBucketMonitorVo(newKey).getCreatingTimeoutSize());
                List<Thread> threadList = os.interruptWaitingThreadsInBucket(newKey);
                Assertions.assertTrue(threadList.contains(firstThread));
            }

            //interruptWaitingThreads()
            firstThread = new ObjectBorrowThread(os, newKey);
            String newKey2 = "Thinking in Rust";
            ObjectBorrowThread secondThread = new ObjectBorrowThread(os, newKey2);
            firstThread.start();
            secondThread.start();
            if (waitUtilWaiting(firstThread)) {
                Thread.sleep(200L);
                List<Thread> threadList = os.interruptWaitingThreadsInBuckets();
                Assertions.assertTrue(threadList.contains(firstThread));
                Assertions.assertTrue(threadList.contains(secondThread));
            }
        }
    }

    @Test
    public void testAutoInterruptBlockInFactory() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setMaxWait(1L);//1 millisecond
        config.setIntervalOfClearTimeout(10L);
        TextBookFactory bookFactory = new TextBookFactory();
        config.setObjectFactory(bookFactory);
        bookFactory.addFactoryMethodPauseTime("create", Long.valueOf(Long.MAX_VALUE));

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            String newKey = "Thinking in C++";
            ObjectBorrowThread firstThread = new ObjectBorrowThread(os, newKey);
            firstThread.start();
            firstThread.join();

            Assertions.assertNotNull(firstThread.getFailureCause());
            BeeObjectBucketMonitorVo bucketMonitorVo = os.getBucketMonitorVo(bookFactory.getDefaultKey());
            Assertions.assertEquals(0, bucketMonitorVo.getCreatingSize());
            Assertions.assertEquals(0, bucketMonitorVo.getCreatingTimeoutSize());
        }
    }
}
