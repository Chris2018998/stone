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
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0056PoolKeyThreadLocalTest {

    @Test
    public void testDisableThreadLocal() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setUseThreadLocal(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
        }

        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createDefault();
        config2.setUseThreadLocal(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config2)) {
            Object object1, object2;
            try (BeeObjectHandle<String, Book> handle1 = os.getObjectHandle()) {
                object1 = TestUtil.getFieldValue(handle1, "p");
            }

            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                object2 = TestUtil.getFieldValue(handle2, "p");
            }
            Assertions.assertEquals(object1, object2);
        }
    }
}
