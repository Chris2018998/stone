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
    public void testCheckLazy() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            //1: state check
            Assertions.assertTrue(os.isLazy());
            BeeObjectPoolMonitorVo poolMonitorVo = os.getPoolMonitorVo(true);
            Assertions.assertTrue(poolMonitorVo.isLazy());
            Assertions.assertFalse(poolMonitorVo.isReady());
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVos());//no keys
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVo("Any"));

            //2: exception check when method call on os
            try {
                os.enableLogPrinter(true);
                Assertions.fail("<ObjectSourceLazyTest>Test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals("No operations allowed on lazy pool", e.getMessage());
                Assertions.assertEquals("Pool is lazy and it can be initialized by calling getObjectHandle method of objectSource", os.toString());
            }
        }
    }

    @Test
    public void testLazyPoolStart() throws Exception {
        //1: startup by calling getObjectHandle()
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            Assertions.assertTrue(os.isLazy());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
            Assertions.assertFalse(os.isLazy());
        }

        //2: startup by calling getObjectHandle(new key)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            Assertions.assertTrue(os.isLazy());
            String newKey = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey)) {
                Assertions.assertNotNull(ignored);
            }
            Assertions.assertFalse(os.isLazy());
        }

        //3: startup by calling getObjectHandle(default key)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            TextBookFactory bookFactory = new TextBookFactory();
            os.setObjectFactory(bookFactory);
            Assertions.assertTrue(os.isLazy());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(bookFactory.getDefaultKey())) {
                Assertions.assertNotNull(ignored);
            }
            Assertions.assertFalse(os.isLazy());
        }

        //4: startup by calling getObjectHandle(default key)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            TextBookFactory bookFactory = new TextBookFactory();
            os.setObjectFactory(bookFactory);
            Assertions.assertTrue(os.isLazy());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(new String(bookFactory.getDefaultKey().getBytes()))) {
                Assertions.assertNotNull(ignored);
            }
            Assertions.assertFalse(os.isLazy());
        }
    }
}




