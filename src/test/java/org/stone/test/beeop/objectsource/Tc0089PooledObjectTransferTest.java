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
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0089PooledObjectTransferTest {

    @Test
    public void testTransferPooledObject() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            ObjectBorrowThread borrowThread = new ObjectBorrowThread(os);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                borrowThread.start();
            }
            borrowThread.join();
            Assertions.assertNotNull(borrowThread.getObjectHale());
        }
    }

    @Test
    public void testTransferException() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setParkTimeForRetry(1L);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            ObjectBorrowThread borrowThread = new ObjectBorrowThread(os);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                borrowThread.start();
                if (TestUtil.waitUtilWaiting(borrowThread)) {
                    os.clearBucketObjects(config.getObjectFactory().getDefaultKey(), true);
                }
            }
            borrowThread.join();
            Assertions.assertNotNull(borrowThread.getFailureCause());
        }
    }
}
