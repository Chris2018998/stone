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
import org.stone.beeop.*;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0049DefaultKeyTest {

    @Test
    public void testGetWithDefaultKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> factory = config.getObjectFactory();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {

            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }

            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle(factory.getDefaultKey())) {
                Assertions.assertNotNull(handle);
            }

            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle(new String(factory.getDefaultKey().getBytes()))) {
                Assertions.assertNotNull(handle);
            }
        }
    }

    @Test
    public void testDeleteDefaultKey() {
        //default key forbidden to deleted
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> factory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try {
                os.deleteKey(factory.getDefaultKey());
                Assertions.fail("[Tc0049DefaultKeyTest.testDeleteDefaultKey]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Default key is forbidden to delete", e.getMessage());
            }

            try {
                os.deleteKey(factory.getDefaultKey(), true);
                Assertions.fail("[Tc0049DefaultKeyTest.testDeleteDefaultKey]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Default key is forbidden to delete", e.getMessage());
            }
        }
    }

    @Test
    public void testClearObjectWithDefaultKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        BeeObjectFactory<String, Book> factory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectKeyMonitorVo defaultKeyMonitor = os.getKeyMonitorVo(factory.getDefaultKey());
            Assertions.assertEquals(1, defaultKeyMonitor.getIdleSize());
            Assertions.assertEquals(0, defaultKeyMonitor.getBorrowedSize());

            //clear1
            os.clearKeyObjects(factory.getDefaultKey());
            defaultKeyMonitor = os.getKeyMonitorVo(factory.getDefaultKey());
            Assertions.assertEquals(0, defaultKeyMonitor.getIdleSize());
            Assertions.assertEquals(0, defaultKeyMonitor.getBorrowedSize());

            //add new object
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(factory.getDefaultKey())) {
                Assertions.assertEquals(1, os.getKeyMonitorVo(factory.getDefaultKey()).getBorrowedSize());
            }
            Assertions.assertEquals(1, os.getKeyMonitorVo(factory.getDefaultKey()).getIdleSize());
            Assertions.assertEquals(0, os.getKeyMonitorVo(factory.getDefaultKey()).getBorrowedSize());

            //clear2
            os.clearKeyObjects(factory.getDefaultKey(), false);
            defaultKeyMonitor = os.getKeyMonitorVo(factory.getDefaultKey());
            Assertions.assertEquals(0, defaultKeyMonitor.getIdleSize());
            Assertions.assertEquals(0, defaultKeyMonitor.getBorrowedSize());
        }
    }
}
