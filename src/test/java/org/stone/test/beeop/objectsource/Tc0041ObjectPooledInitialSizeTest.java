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
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0041ObjectPooledInitialSizeTest {

    @Test
    public void testInitialization() throws Exception {
        TextBookFactory bookFactory = new TextBookFactory();
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setObjectFactory(bookFactory);
        String defaultKey = bookFactory.getDefaultKey();

        config.setInitialSize(1);
        config.setAsyncCreateInitObjects(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.getKeyMonitorVo(defaultKey).getIdleSize());
        }
        config.setInitialSize(0);
        config.setAsyncCreateInitObjects(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(0, os.getKeyMonitorVo(defaultKey).getIdleSize());
        }

        config.setInitialSize(1);
        config.setAsyncCreateInitObjects(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }

        config.setInitialSize(0);
        config.setAsyncCreateInitObjects(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }

        config.setInitialSize(1);
        config.setAsyncCreateInitObjects(true);
        bookFactory.setException(new Exception("Failed to create book instance"));
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }
    }
}
