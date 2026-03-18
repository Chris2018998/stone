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
import org.stone.beeop.exception.BeeObjectSourcePoolNotReadyException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0033ObjectSourcePoolSuspendTest {

    @Test
    public void testSuspended() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(os.suspend());
            Assertions.assertTrue(os.getPoolMonitorVo(false).isSuspended());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("Suspended test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, e);
            }

            Assertions.assertTrue(os.resume());
            Assertions.assertFalse(os.getPoolMonitorVo(false).isSuspended());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
        }
    }


}
