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
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.BlockWayTypes;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.List;

import static org.stone.test.base.TestUtil.waitUtilWaiting;

/**
 * @author Chris Liao
 */
public class Tc0054PoolKeySemaphoreTest {

    @Test
    public void testWaitTimeout() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setMaxActive(1);
        config.setInitialSize(0);
        config.setSemaphoreSize(1);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(1L);
        TextBookFactory factory = new TextBookFactory();
        factory.setBlock(BlockWayTypes.Type_Sleep, 1000L);
        config.setObjectFactory(factory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //1: create first borrow thread to get connection
            ObjectBorrowThread firstBorrower = new ObjectBorrowThread(os);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation

                ObjectBorrowThread secondBorrower = new ObjectBorrowThread(os);
                secondBorrower.start();
                secondBorrower.join();

                Assertions.assertTrue(secondBorrower.getFailureCause().getMessage().contains("Waited timeout on key semaphore"));
                firstBorrower.interrupt();
            }
        }
    }

    @Test
    public void testInterruptWaiters() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setMaxActive(1);
        config.setInitialSize(0);
        config.setSemaphoreSize(1);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(Long.MAX_VALUE);
        TextBookFactory factory = new TextBookFactory();
        factory.setBlock(BlockWayTypes.Type_Sleep, Long.MAX_VALUE);
        config.setObjectFactory(factory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //1: create first borrow thread to get connection
            ObjectBorrowThread firstBorrower = new ObjectBorrowThread(os);
            firstBorrower.start();

            //2: attempt to get connection in current thread
            if (waitUtilWaiting(firstBorrower)) {//block 1 second in pool instance creation
                ObjectBorrowThread secondBorrower = new ObjectBorrowThread(os);
                secondBorrower.start();
                if (waitUtilWaiting(secondBorrower)) {//block 1 second in pool instance creation
                    List<Thread> interruptedThreads = os.interruptWaitingThreads(factory.getDefaultKey());
                    Assertions.assertTrue(interruptedThreads.contains(firstBorrower));
                    Assertions.assertTrue(interruptedThreads.contains(secondBorrower));
                }
            }
        }
    }
}
