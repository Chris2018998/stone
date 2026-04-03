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
    public void testLazyPool() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            //1: state check
            Assertions.assertTrue(os.isLazy());
            BeeObjectPoolMonitorVo poolMonitorVo = os.getPoolMonitorVo(true);
            Assertions.assertTrue(poolMonitorVo.isLazy());
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVos());//no keys
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVo("Any"));

            //2: method call check when os pool is lazy
            try {
                os.enableLogPrinter(true);
                Assertions.fail("<ObjectSourceLazyTest>Test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals("No operations allowed on lazy pool", e.getMessage());
                Assertions.assertEquals("Pool is lazy and it can be initialized by calling getObjectHandle method of objectSource", os.toString());
            }

            //3: initialize object source pool
            os.setObjectFactory(new TextBookFactory());
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }

            //4: check pool status
            Assertions.assertFalse(os.isLazy());
            Assertions.assertFalse((os.getPoolMonitorVo(false).isLazy()));

            //5: method call check
            try {
                os.enableLogPrinter(true);
            } catch (Throwable e) {
                Assertions.fail("Tc0029ObjectSourcePoolLazyTest.testLazyPool]test failed");
            }
        }
    }
}




