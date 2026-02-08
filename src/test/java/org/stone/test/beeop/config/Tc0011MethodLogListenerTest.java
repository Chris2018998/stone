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
import org.stone.beeop.BeeMethodLogListener;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.methodLog.LogListener1;
import org.stone.test.beeop.objects.methodLog.LogListener2;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0011MethodLogListenerTest {

    @Test
    public void testSetGet() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        BeeMethodLogListener<String> logListener = new LogListener1();
        config.setLogListener(logListener);
        Assertions.assertEquals(logListener, config.getLogListener());

        config.setLogListenerClass(LogListener1.class);
        Assertions.assertEquals(LogListener1.class, config.getLogListenerClass());

        config.setLogListenerClassName(LogListener1.class.getName());
        Assertions.assertEquals(LogListener1.class.getName(), config.getLogListenerClassName());
    }

    @Test
    public void testCheckPass() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        //1:log listener instance
        BeeMethodLogListener<String> logListener = new LogListener1();
        config.setLogListener(logListener);
        BeeObjectSourceConfig<String, Book> checkedConfig = config.check();
        Assertions.assertEquals(logListener, checkedConfig.getLogListener());

        //2:log listener class
        config = OsConfigFactory.createDefault();
        config.setLogListenerClass(LogListener1.class);
        checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getLogListener());

        //3:log listener class name
        config = OsConfigFactory.createDefault();
        config.setLogListenerClassName(LogListener1.class.getName());
        checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getLogListener());
    }

    @Test
    public void testCheckFailure() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config = OsConfigFactory.createDefault();

        //1:class not found
        String listenerClassName = LogListener1.class.getName() + "NotFound";
        config.setLogListenerClassName(listenerClassName);
        try {
            config.check();
            fail("Setting test failed on configuration item[listener-class-name]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create log listener with class:"));
        }

        listenerClassName = LogListener2.class.getName();
        config.setLogListenerClassName(LogListener2.class.getName());
        try {
            config.check();
            fail("Setting test failed on configuration item[listener-class-name]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create log listener with class:"));
        }
    }
}
