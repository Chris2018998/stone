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
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0062ObjectCallLogsTest {

    @Test
    public void testCallLog() throws Throwable {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setEnableLogCache(true);
        config.addObjectMethodName("getAuthor");
        config.addObjectMethodName("setAuthor");
        BeeObjectFactory<String, Book> objectFactory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                Assertions.assertTrue(os.getKeyObjectLogs(objectFactory.getDefaultKey()).isEmpty());
                bookHandle.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce Eckel2"});
                Assertions.assertEquals(1, os.getKeyObjectLogs(objectFactory.getDefaultKey()).size());

                os.clearKeyObjectLogs(objectFactory.getDefaultKey());
                Assertions.assertTrue(os.getKeyObjectLogs(objectFactory.getDefaultKey()).isEmpty());

                os.enableLogCache(objectFactory.getDefaultKey(), false);
                bookHandle.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce Eckel2"});
                Assertions.assertTrue(os.getKeyObjectLogs(objectFactory.getDefaultKey()).isEmpty());
            }
        }
    }
}
