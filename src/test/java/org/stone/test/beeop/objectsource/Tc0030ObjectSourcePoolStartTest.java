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
public class Tc0030ObjectSourcePoolStartTest {

    @Test
    public void testPoolStartWithConfigObject() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(OsConfigFactory.createDefault())) {
            Assertions.assertFalse(os.isLazy());

            BeeObjectPoolMonitorVo poolMonitorVo2 = os.getPoolMonitorVo(true);
            Assertions.assertFalse(poolMonitorVo2.isLazy());
            Assertions.assertTrue(poolMonitorVo2.isReady());
            Assertions.assertEquals("Pool is ready", os.toString());
            Assertions.assertNotNull(poolMonitorVo2.getBucketMonitorVos());
            Assertions.assertNotNull(poolMonitorVo2.getBucketMonitorVo(os.getObjectFactory().getDefaultKey()));

            try {
                os.enableLogPrinter(true);
            } catch (Exception e) {
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testLazyPool]failed");
            }
        }
    }

    @Test
    public void testLazyPool() throws Exception {
        //Not set configuration to os
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            //1: check state of pool
            Assertions.assertTrue(os.isLazy());
            Assertions.assertEquals("Pool is lazy and it can be initialized by calling getObjectHandle method of objectSource", os.toString());

            //2: monitor vo check
            BeeObjectPoolMonitorVo poolMonitorVo = os.getPoolMonitorVo(true);
            Assertions.assertTrue(poolMonitorVo.isLazy());
            Assertions.assertFalse(poolMonitorVo.isReady());
            Assertions.assertNull(poolMonitorVo.getBucketMonitorVos());//no keys
            Assertions.assertNull(poolMonitorVo.getBucketMonitorVo("Any"));

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
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testLazyPool]failed");
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
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testLazyPool]failed");
            }
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(bookFactory.getDefaultKey())) {//getObjectHandle(default key)
                Assertions.assertNotNull(ignored);
            } catch (Exception e) {
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testLazyPool]failed");
            }

            //8.1: re-check state of pool
            Assertions.assertFalse(os.isLazy());
            //8.2: monitor vo check
            BeeObjectPoolMonitorVo poolMonitorVo2 = os.getPoolMonitorVo(true);
            Assertions.assertFalse(poolMonitorVo2.isLazy());
            Assertions.assertTrue(poolMonitorVo2.isReady());
            Assertions.assertNotNull(poolMonitorVo2.getBucketMonitorVos());
            Assertions.assertNotNull(poolMonitorVo2.getBucketMonitorVo(bookFactory.getDefaultKey()));
            //8.3: exception check when call method on os
            try {
                os.enableLogPrinter(true);
            } catch (Exception e) {
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testLazyPool]failed");
            }
        }
    }

    @Test
    public void testLazyPoolByNewKey() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            Assertions.assertTrue(os.isLazy());
            TextBookFactory bookFactory = new TextBookFactory();
            os.setObjectFactory(bookFactory);

            String newKey = "Thinking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey)) {
                Assertions.assertTrue(os.getPoolMonitorVo(false).isReady());
                Assertions.assertTrue(os.existsBucket(bookFactory.getDefaultKey()));
                Assertions.assertTrue(os.existsBucket(newKey));
            }
        }
    }
}




