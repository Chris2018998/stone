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
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0063ObjectAbortTest {

    @Test
    public void testEviction() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectHandle<String, Book> handleTemp = null;
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                handleTemp = handle;
                handle.abort();
            } catch (Throwable e) {
                Assertions.assertNotNull(handleTemp);
                Assertions.assertTrue(handleTemp.isClosed());
            }
        }
    }
}
