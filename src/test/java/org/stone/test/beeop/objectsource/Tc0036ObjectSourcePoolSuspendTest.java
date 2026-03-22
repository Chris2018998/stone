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
import org.stone.beeop.exception.BeeObjectSourcePoolNotReadyException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0036ObjectSourcePoolSuspendTest {

    @Test
    public void testSuspended() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> bookObjectFactory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(os.suspend());
            Assertions.assertTrue(os.getPoolMonitorVo(false).isSuspended());

            //check1: getObjectHandle
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("Suspended test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, e);
            }

            //check2: getKeyMonitorVo
            try {
                os.getKeyMonitorVo(bookObjectFactory.getDefaultKey());
                Assertions.fail("Suspended test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, e);
            }

            //resume pool
            Assertions.assertTrue(os.resume());
            Assertions.assertFalse(os.getPoolMonitorVo(false).isSuspended());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
        }
    }

    @Test
    public void testSuspendKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> bookObjectFactory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(os.suspendKey(bookObjectFactory.getDefaultKey()));
            Assertions.assertTrue(os.getKeyMonitorVo(bookObjectFactory.getDefaultKey()).isSuspended());

            Assertions.assertTrue(os.resumeKey(bookObjectFactory.getDefaultKey()));
            Assertions.assertFalse(os.getKeyMonitorVo(bookObjectFactory.getDefaultKey()).isSuspended());
        }
    }
}
