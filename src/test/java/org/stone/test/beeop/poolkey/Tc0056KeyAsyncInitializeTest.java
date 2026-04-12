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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0056KeyAsyncInitializeTest {

    @Test
    public void testAsyncInitialization() {
        //0: async success
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(0);
        config.setAsyncCreateInitObjects(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }

        //1: async success
        config.setInitialSize(1);
        config.setAsyncCreateInitObjects(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }

        //2: async failed
        config.setInitialSize(1);
        config.setAsyncCreateInitObjects(true);
        TextBookFactory bookFactory = new TextBookFactory();
        config.setObjectFactory(bookFactory);
        bookFactory.setException(new Exception("Failed to create book instance"));
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }
    }
}
