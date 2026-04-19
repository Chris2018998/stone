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
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.beeop.exception.BeeObjectSourcePoolLazyInitializationException;
import org.stone.beeop.exception.BeeObjectSourcePoolStartedFailureException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 * <p>
 * 1: pool start in constructor of object source
 * 2: pool start by calling getObjectHandle() method or  getObjectHandle(Key) method
 */
public class Tc0029ObjectSourcePoolStartTest {

    @Test
    public void testPoolStartWithConfigObject() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(OsConfigFactory.createDefault())) {
            Assertions.assertFalse(os.isLazy());
            BeeObjectPoolMonitorVo poolMonitorVo2 = os.getPoolMonitorVo(true);
            Assertions.assertFalse(poolMonitorVo2.isLazy());
            Assertions.assertTrue(poolMonitorVo2.isReady());
            Assertions.assertNotNull(poolMonitorVo2.getKeyMonitorVos());
            Assertions.assertNotNull(poolMonitorVo2.getKeyMonitorVo(os.getObjectFactory().getDefaultKey()));

            try {
                os.enableLogPrinter(true);
            } catch (Exception e) {
                Assertions.fail("[Tc0029ObjectSourcePoolStartTest.testLazyPool]failed");
            }
        }
    }

    @Test
    public void testLazyPool() throws Exception {

        //Not set configuration to os
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            //1: check state of pool
            Assertions.assertTrue(os.isLazy());

            //2: monitor vo check
            BeeObjectPoolMonitorVo poolMonitorVo = os.getPoolMonitorVo(true);
            Assertions.assertTrue(poolMonitorVo.isLazy());
            Assertions.assertFalse(poolMonitorVo.isReady());
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVos());//no keys
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVo("Any"));

            //3: exception check when call method on os
            try {
                os.enableLogPrinter(true);
                Assertions.fail("<ObjectSourceLazyTest>Test failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolLazyInitializationException.class, e);
                Assertions.assertEquals("No operations allowed on lazy pool", e.getMessage());
                Assertions.assertEquals("Pool is lazy and it can be initialized by calling getObjectHandle method of objectSource", os.toString());
            }

            //4: attempt to call getObjectHandle() when not fill Object factory
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0029ObjectSourcePoolStartTest.testLazyPool]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e);
                Assertions.assertInstanceOf(BeeObjectSourceConfigException.class, e.getCause());
                Assertions.assertEquals("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]", e.getCause().getMessage());
            }

            //6: set object factory to os
            TextBookFactory bookFactory = new TextBookFactory();
            os.setObjectFactory(bookFactory);

            //7: set object factory to os
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {//getObjectHandle()
                Assertions.assertNotNull(ignored);
            } catch (Exception e) {
                Assertions.fail("[Tc0029ObjectSourcePoolStartTest.testLazyPool]failed");
            }
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(bookFactory.getDefaultKey())) {//getObjectHandle(default key)
                Assertions.assertNotNull(ignored);
            } catch (Exception e) {
                Assertions.fail("[Tc0029ObjectSourcePoolStartTest.testLazyPool]failed");
            }

            //8.1: re-check state of pool
            Assertions.assertFalse(os.isLazy());
            //8.2: monitor vo check
            BeeObjectPoolMonitorVo poolMonitorVo2 = os.getPoolMonitorVo(true);
            Assertions.assertFalse(poolMonitorVo2.isLazy());
            Assertions.assertTrue(poolMonitorVo2.isReady());
            Assertions.assertNotNull(poolMonitorVo2.getKeyMonitorVos());
            Assertions.assertNotNull(poolMonitorVo2.getKeyMonitorVo(bookFactory.getDefaultKey()));
            //8.3: exception check when call method on os
            try {
                os.enableLogPrinter(true);
            } catch (Exception e) {
                Assertions.fail("[Tc0029ObjectSourcePoolStartTest.testLazyPool]failed");
            }
        }
    }
}




