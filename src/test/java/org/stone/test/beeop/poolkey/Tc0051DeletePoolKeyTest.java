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
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectKeyMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0051DeletePoolKeyTest {

    @Test
    public void testDeleteKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setParkTimeForRetry(1L);//import: time control

        String defaultKey = objectFactory.getDefaultKey();
        String key2 = "Thanking in Rust";
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertTrue(os.existsKey(defaultKey));

            //1: try to delete default key
            try {
                os.deleteKey(defaultKey);
                Assertions.fail("Failed to run testcase:[testDeletePooledKey)");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Default key is forbidden to delete", e.getMessage());
            }

            //delete key
            Assertions.assertFalse(os.existsKey(key2));
            Assertions.assertFalse(os.deleteKey(key2));

            //2: add a new pooled key
            BeeObjectHandle<String, Book> rustBookHandle = os.getObjectHandle(key2);
            rustBookHandle.close();
            Assertions.assertEquals(2, os.keySize());
            Assertions.assertTrue(os.existsKey(key2));
            Assertions.assertTrue(os.deleteKey(key2));
            Assertions.assertEquals(1, os.keySize());
            Assertions.assertFalse(os.existsKey(key2));

            //3: test delete key by force
            os.getObjectHandle(key2);
            Assertions.assertTrue(os.existsKey(key2));
            BeeObjectKeyMonitorVo rustKeyMonitorVo = os.getKeyMonitorVo(key2);
            Assertions.assertTrue(rustKeyMonitorVo.isReady());
            Assertions.assertEquals(0, rustKeyMonitorVo.getIdleSize());
            Assertions.assertEquals(1, rustKeyMonitorVo.getBorrowedSize());
            Assertions.assertTrue(os.deleteKey(key2, true));
            Assertions.assertFalse(os.existsKey(key2));
        }
    }
}
