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
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeePooledObjectGetTimeoutException;
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0057PoolKeyWaitQueueTest {
    @Test
    public void testTransfer() throws Exception {
        //1: wait timeout test
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setMaxWait(1L);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                try (BeeObjectHandle<String, Book> ignored1 = os.getObjectHandle()) {
                } catch (Exception e) {
                    Assertions.assertInstanceOf(BeePooledObjectGetTimeoutException.class, e);
                }
            }
        }

        //2: transfer test
        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createDefault();
        config2.setInitialSize(1);
        config2.setMaxActive(1);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config2)) {
            BeeObjectHandle<String, Book> handle = null;
            try {
                handle = os.getObjectHandle();
                ObjectBorrowThread borrowThread = new ObjectBorrowThread(os);
                borrowThread.start();
                if (TestUtil.waitUtilWaiting(borrowThread)) {
                    handle.close();
                    Thread.sleep(100L);
                    borrowThread.join();
                }
            } finally {
                if (handle != null) {
                    handle.close();
                }
            }
        }
    }
}
