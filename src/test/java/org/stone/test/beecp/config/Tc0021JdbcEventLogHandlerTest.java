/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeJdbcEventLogManager;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.jdbclog.MockJdbcEventLogHandler;
import org.stone.test.beecp.objects.jdbclog.MockJdbcEventLogHandler2;
import org.stone.test.beecp.objects.jdbclog.MockJdbcEventLogManager;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0021JdbcEventLogHandlerTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertTrue(config.isEventLogHandledBySyncMode());//default check
        config.setEventLogHandledBySyncMode(false);
        Assertions.assertFalse(config.isEventLogHandledBySyncMode());

        Assertions.assertNull(config.getEventLogHandler());//default check
        config.setEventLogHandler(new MockJdbcEventLogHandler());
        Assertions.assertNotNull(config.getEventLogHandler());//default check
        config.setEventLogHandler(null);
        Assertions.assertNull(config.getEventLogHandler());//default check

        Assertions.assertNull(config.getEventLogHandlerClass());//default check
        config.setEventLogHandlerClass(MockJdbcEventLogHandler.class);
        Assertions.assertNotNull(config.getEventLogHandlerClass());
        config.setEventLogHandlerClass(null);
        Assertions.assertNull(config.getEventLogHandlerClass());

        Assertions.assertNull(config.getEventLogHandlerClassName());//default check
        config.setEventLogHandlerClassName(MockJdbcEventLogHandler.class.getName());
        Assertions.assertNotNull(config.getEventLogHandlerClassName());
        config.setEventLogHandlerClassName(null);
        Assertions.assertNull(config.getEventLogHandlerClassName());
    }

    @Test
    public void testCheckFailed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeJdbcEventLogManager logManager = new MockJdbcEventLogManager();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setEventLogManager(logManager);
        config1.setEventLogHandlerClassName(MockJdbcEventLogHandler2.class.getName());//class can not be
        try {
            config1.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Throwable cause1 = e.getCause();
            Assertions.assertInstanceOf(BeanException.class, cause1);
            Assertions.assertInstanceOf(NoSuchMethodException.class, cause1.getCause());
        }

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setEventLogManager(logManager);
        config2.setEventLogHandlerClassName(MockJdbcEventLogHandler2.class.getName() + "_NOT");//class not found
        try {
            config2.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }
    }

    @Test
    public void testCheckPassed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        MockJdbcEventLogManager logManager = new MockJdbcEventLogManager();

        //1: instance
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(connectionFactory);
        config1.setEventLogManager(logManager);
        MockJdbcEventLogHandler handler = new MockJdbcEventLogHandler();
        config1.setEventLogHandler(handler);
        try {
            BeeDataSourceConfig checkedConfig = config1.check();
            Assertions.assertEquals(handler, checkedConfig.getEventLogHandler());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setEventLogManager(logManager);
        config2.setEventLogHandlerClass(MockJdbcEventLogHandler.class);
        try {
            config2.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(connectionFactory);
        config3.setEventLogManager(logManager);
        config3.setEventLogHandlerClassName(MockJdbcEventLogHandler.class.getName());
        try {
            config3.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }
    }

    @Test
    public void testCheckPassedWithoutEventLogManager() throws Exception {
        BeeDataSourceConfig config1 = createEmpty();
        BeeConnectionFactory connectionFactory = new MockConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        config1.setEventLogHandler(new MockJdbcEventLogHandler());
        BeeDataSourceConfig config11 = config1.check();
        Assertions.assertNull(config11.getEventLogHandler());

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setEventLogHandlerClass(MockJdbcEventLogHandler.class);
        BeeDataSourceConfig config21 = config2.check();
        Assertions.assertNull(config21.getEventLogHandler());

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setEventLogHandlerClassName(MockJdbcEventLogHandler.class.getName());
        BeeDataSourceConfig config31 = config3.check();
        Assertions.assertNull(config31.getEventLogHandler());

        //test set a log collector to config object
        BeeJdbcEventLogManager logManager = new MockJdbcEventLogManager();
        config1.setEventLogManager(logManager);
        config11 = config1.check();
        Assertions.assertNotNull(config11.getEventLogHandler());

        config2.setEventLogManager(logManager);
        config21 = config2.check();
        Assertions.assertNotNull(config21.getEventLogHandler());

        config3.setEventLogManager(logManager);
        config31 = config3.check();
        Assertions.assertNotNull(config31.getEventLogHandler());
    }
}
