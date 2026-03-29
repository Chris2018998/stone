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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourcePoolNotReadyException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0053SuspendPooledKeyTest {

    @Test
    public void testSuspendKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setParkTimeForRetry(1L);//import: time control

        String defaultKey = objectFactory.getDefaultKey();
        String key2 = "Thanking in Rust";

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectHandle<String, Book> handle1 = os.getObjectHandle();
            BeeObjectHandle<String, Book> handle2 = os.getObjectHandle(key2);
            handle1.close();
            handle2.close();
            Assertions.assertTrue(os.suspendKey(defaultKey));
            Assertions.assertTrue(os.suspendKey(key2));

            try {
                os.getObjectHandle();
                Assertions.fail("testSuspendKey test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, e);
            }
            Assertions.assertTrue(os.getKeyMonitorVo(defaultKey).isSuspended());

            try {
                os.getObjectHandle(key2);
                Assertions.fail("testSuspendKey test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolNotReadyException.class, e);
            }
            Assertions.assertTrue(os.getKeyMonitorVo(key2).isSuspended());

            Assertions.assertTrue(os.resumeKey(defaultKey));
            Assertions.assertTrue(os.resumeKey(key2));
            handle1 = os.getObjectHandle();
            Assertions.assertNotNull(handle1);
            handle2 = os.getObjectHandle(key2);
            Assertions.assertNotNull(handle2);
            handle1.close();
            handle2.close();
        }
    }

}



