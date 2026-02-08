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
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeMethodLogListenerFactory;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.methodLog.LogListenerFactory1;
import org.stone.test.beeop.objects.methodLog.LogListenerFactory2;
import org.stone.test.beeop.objects.methodLog.LogListenerFactory3;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0012MethodLogListenerFactoryTest {

    @Test
    public void testSetGet() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        LogListenerFactory1 logListenerFactory = new LogListenerFactory1();
        config.setLogListenerFactory(logListenerFactory);
        Assertions.assertEquals(logListenerFactory, config.getLogListenerFactory());

        config.setLogListenerFactoryClass(LogListenerFactory1.class);
        Assertions.assertEquals(LogListenerFactory1.class, config.getLogListenerFactoryClass());

        config.setLogListenerFactoryClassName(LogListenerFactory1.class.getName());
        Assertions.assertEquals(LogListenerFactory1.class.getName(), config.getLogListenerFactoryClassName());
    }

    @Test
    public void testCheckPass() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        //1:log listener factory instance
        BeeMethodLogListenerFactory<String> logListenerFactory = new LogListenerFactory1();
        config.setLogListenerFactory(logListenerFactory);
        BeeObjectSourceConfig<String, Book> checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getLogListener());

        //2:log listener factory class
        config = OsConfigFactory.createDefault();
        config.setLogListenerFactoryClass(LogListenerFactory1.class);
        checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getLogListener());

        //3:log listener factory class name
        config = OsConfigFactory.createDefault();
        config.setLogListenerFactoryClassName(LogListenerFactory1.class.getName());
        checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getLogListener());
    }

    @Test
    public void testCheckFailure() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();

        //1:class not found
        String listenerFactoryClassName = LogListenerFactory2.class.getName() + "NotFound";
        config.setLogListenerFactoryClassName(listenerFactoryClassName);
        try {
            config.check();
            fail("Setting test failed on configuration item[listener-factory-class-name]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create log listener factory with class:"));
        }

        //2:No default constructor
        listenerFactoryClassName = LogListenerFactory2.class.getName();
        config.setLogListenerFactoryClassName(listenerFactoryClassName);
        try {
            config.check();
            fail("Setting test failed on configuration item[listener-factory-class-name]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create log listener factory with class:"));
        }
    }

    @Test
    public void testCheckFailureToCreateLogListener() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setLogListenerFactory(new LogListenerFactory3());
        try {
            config.check();
            fail("Setting test failed on configuration item[listener-factory-class-name]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create log listener by factory"));
        }

        config = OsConfigFactory.createDefault();
        config.setLogListenerFactoryClass(LogListenerFactory3.class);
        try {
            config.check();
            fail("Setting test failed on configuration item[listener-factory-class-name]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create log listener by factory"));
        }
    }
}
