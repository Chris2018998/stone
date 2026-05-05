/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.pool.BookPool;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */

public class Tc0002PoolSimpleSetTest {
    private static BeeObjectSourceConfig<String, Book> config;

    @BeforeAll
    public static void createConfig() {
        config = OsConfigFactory.createEmpty();
    }

    @Test
    public void testPoolName() {
        config.setPoolName("pool1");
        Assertions.assertEquals("pool1", config.getPoolName());
        config.setObjectFactoryClass(TextBookFactory.class);
        BeeObjectSourceConfig<String, Book> checkedConfig = config.check();
        Assertions.assertEquals(config.getPoolName(), checkedConfig.getPoolName());

        config.setPoolName(null);
        Assertions.assertNull(config.getPoolName());
        checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getPoolName());
    }

    @Test
    public void testOnSetAndGet() {
        config.setFairMode(true);
        Assertions.assertTrue(config.isFairMode());

        config.setFairMode(false);
        Assertions.assertFalse(config.isFairMode());
    }

    @Test
    public void testSemaphoreSize() {
        config.setSemaphoreSize(5);
        Assertions.assertEquals(5, config.getSemaphoreSize());

        //semaphoreSize
        try {
            config.setSemaphoreSize(-1);
            fail("Setting test failed on configuration item[semaphore-size]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'borrow-semaphore-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setSemaphoreSize(0);
            fail("Setting test failed on configuration item[semaphore-size]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'borrow-semaphore-size' must be greater than zero", e.getMessage());
        }
    }

    @Test
    public void testThreadLocalSetting() {
        config.setUseThreadLocal(true);
        Assertions.assertTrue(config.isUseThreadLocal());

        config.setUseThreadLocal(false);
        Assertions.assertFalse(config.isUseThreadLocal());
    }

    @Test
    public void testInitModeSetting() {
        config.setAsyncCreateInitObjects(true);
        Assertions.assertTrue(config.isAsyncCreateInitObjects());

        config.setAsyncCreateInitObjects(false);
        Assertions.assertFalse(config.isAsyncCreateInitObjects());
    }

    @Test
    public void testRegisterMbeansSetting() {
        config.setRegisterMbeans(true);
        Assertions.assertTrue(config.isRegisterMbeans());

        config.setRegisterMbeans(false);
        Assertions.assertFalse(config.isRegisterMbeans());
    }

    @Test
    public void testRegisterJvmHoo() {
        Assertions.assertTrue(config.isRegisterJvmHook());//default check
        config.setRegisterJvmHook(false);
        Assertions.assertFalse(config.isRegisterJvmHook());
        config.setRegisterJvmHook(true);
        Assertions.assertTrue(config.isRegisterJvmHook());
    }

    @Test
    public void testLogsPrintSetting() {
        config.setPrintRuntimeLogs(true);
        Assertions.assertTrue(config.isPrintRuntimeLogs());

        config.setPrintConfiguration(true);
        Assertions.assertTrue(config.isPrintConfiguration());

        config.setPrintRuntimeLogs(false);
        Assertions.assertFalse(config.isPrintRuntimeLogs());

        config.setPrintConfiguration(false);
        Assertions.assertFalse(config.isPrintConfiguration());
    }

    @Test
    public void testPoolClassName() {
        config.setPoolImplementClassName(BookPool.class.getName());
        Assertions.assertEquals(BookPool.class.getName(), config.getPoolImplementClassName());
        config.setPoolImplementClassName(null);
        Assertions.assertNull(config.getPoolImplementClassName());
    }

    @Test
    public void testMethodNameSetting() {
        String methodName = "getTitle";
        Assertions.assertNull(config.getObjectMethodNameList());
        config.removeObjectMethodName(methodName);
        Assertions.assertNull(config.getObjectMethodNameList());

        try {
            config.addObjectMethodName(null);
            fail("Setting test failed on configuration item[object-method-name-list]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'method-name' can't be null or blank", e.getMessage());
        }
        try {
            config.addObjectMethodName("");
            fail("Setting test failed on configuration item[object-method-name-list]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'method-name' can't be null or blank", e.getMessage());
        }
    }

    @Test
    public void testMethodNameSettingCheck() {
        String methodName = "getTitle";
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig<String, Book> checkedConfig = config.check();
        Assertions.assertNull(checkedConfig.getObjectMethodNameList());

        config.addObjectMethodName(methodName);
        Assertions.assertTrue(config.getObjectMethodNameList().contains(methodName));
        checkedConfig = config.check();
        Assertions.assertTrue(checkedConfig.getObjectMethodNameList().contains(methodName));

        config.addObjectMethodName(methodName);
        Assertions.assertTrue(config.getObjectMethodNameList().contains(methodName));
        Assertions.assertEquals(1, config.getObjectMethodNameList().size());

        config.removeObjectMethodName(methodName);
        Assertions.assertTrue(config.getObjectMethodNameList().isEmpty());
        checkedConfig = config.check();
        Assertions.assertNull(checkedConfig.getObjectMethodNameList());
    }
}
