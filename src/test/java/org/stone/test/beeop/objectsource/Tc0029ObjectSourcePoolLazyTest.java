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
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.exception.BeeObjectSourcePoolLazyInitializationException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0029ObjectSourcePoolLazyTest {

    @Test
    public void testLazyException() throws Exception {
        //test1
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            Assertions.assertTrue(os.isClosed());
            try {
                os.enableLogPrinter(true);
                Assertions.fail("<ObjectSourceLazyTest>Test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals("No operations allowed on lazy pool", e.getMessage());
                Assertions.assertEquals("Pool is lazy and initialized by calling its getObjectHandle method", os.toString());
            }

            BeeObjectPoolMonitorVo poolMonitorVo = os.getPoolMonitorVo(false);
            Assertions.assertTrue(poolMonitorVo.isLazy());
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVos());

            poolMonitorVo = os.getPoolMonitorVo(true);
            Assertions.assertTrue(poolMonitorVo.isLazy());
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVos());
            Assertions.assertEquals(0, os.keySize());

            os.setObjectFactory(new TextBookFactory());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertFalse(os.isClosed());
                Assertions.assertEquals(1, os.keySize());
            }
        }

        //test2
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            String key2 = "Thanking in C++";
            Assertions.assertEquals(0, os.keySize());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(key2)) {
                Assertions.assertEquals(2, os.keySize());
            }
        }
    }
}




