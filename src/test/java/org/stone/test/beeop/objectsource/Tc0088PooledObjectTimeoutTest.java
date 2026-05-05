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
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0088PooledObjectTimeoutTest {

    @Test
    public void testIdleTimeout() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setInitialSize(0);
            os.setMaxActive(1);
            os.setIdleTimeout(1L);
            os.setPrintRuntimeLogs(true);
            os.setIntervalOfClearTimeout(100L);
            os.setObjectFactory(new TextBookFactory());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertEquals(1, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getBorrowedSize());
            }
            Assertions.assertEquals(1, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getIdleSize());

            //wait 1second
            Thread.sleep(200L);
            Assertions.assertEquals(0, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getIdleSize());
        }
    }

    @Test
    public void testHoldTimeout() throws Exception {
        //1: holdTimeout >0L
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setInitialSize(0);
            os.setMaxActive(1);
            os.setHoldTimeout(1L);
            os.setPrintRuntimeLogs(true);
            os.setIntervalOfClearTimeout(100L);
            os.setObjectFactory(new TextBookFactory());

            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertEquals(1, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getBorrowedSize());
                Thread.sleep(200L);
                Assertions.assertEquals(0, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getBorrowedSize());
                Assertions.assertEquals(1, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getIdleSize());
                Assertions.assertTrue(handle.isClosed());
            }
        }

        //2: holdTimeout=0L
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setInitialSize(0);
            os.setMaxActive(1);
            os.setHoldTimeout(0L);
            os.setIntervalOfClearTimeout(100L);
            os.setObjectFactory(new TextBookFactory());

            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertEquals(1, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getBorrowedSize());
                Thread.sleep(200L);
                Assertions.assertEquals(1, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getBorrowedSize());
                Thread.sleep(200L);
                Assertions.assertEquals(1, os.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()).getBorrowedSize());
                Assertions.assertFalse(handle.isClosed());
            }
        }
    }
}
