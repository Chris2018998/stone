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
        Assertions.assertTrue(config.isLogHandledBySyncMode());//default check
        config.setLogHandledBySyncMode(false);
        Assertions.assertFalse(config.isLogHandledBySyncMode());

        Assertions.assertNull(config.getLogHandler());//default check
        config.setLogHandler(new MockJdbcEventLogHandler());
        Assertions.assertNotNull(config.getLogHandler());//default check
        config.setLogHandler(null);
        Assertions.assertNull(config.getLogHandler());//default check

        Assertions.assertNull(config.getLogHandlerClass());//default check
        config.setLogHandlerClass(MockJdbcEventLogHandler.class);
        Assertions.assertNotNull(config.getLogHandlerClass());
        config.setLogHandlerClass(null);
        Assertions.assertNull(config.getLogHandlerClass());

        Assertions.assertNull(config.getLogHandlerClassName());//default check
        config.setLogHandlerClassName(MockJdbcEventLogHandler.class.getName());
        Assertions.assertNotNull(config.getLogHandlerClassName());
        config.setLogHandlerClassName(null);
        Assertions.assertNull(config.getLogHandlerClassName());
    }

    @Test
    public void testCheckFailed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeJdbcEventLogManager logManager = new MockJdbcEventLogManager();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setLogManager(logManager);
        config1.setLogHandlerClassName(MockJdbcEventLogHandler2.class.getName());//class can not be
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
        config2.setLogManager(logManager);
        config2.setLogHandlerClassName(MockJdbcEventLogHandler2.class.getName() + "_NOT");//class not found
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
        config1.setLogManager(logManager);
        MockJdbcEventLogHandler handler = new MockJdbcEventLogHandler();
        config1.setLogHandler(handler);
        try {
            BeeDataSourceConfig checkedConfig = config1.check();
            Assertions.assertEquals(handler, checkedConfig.getLogHandler());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setLogManager(logManager);
        config2.setLogHandlerClass(MockJdbcEventLogHandler.class);
        try {
            config2.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(connectionFactory);
        config3.setLogManager(logManager);
        config3.setLogHandlerClassName(MockJdbcEventLogHandler.class.getName());
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
        config1.setLogHandler(new MockJdbcEventLogHandler());
        BeeDataSourceConfig config11 = config1.check();
        Assertions.assertNull(config11.getLogHandler());

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setLogHandlerClass(MockJdbcEventLogHandler.class);
        BeeDataSourceConfig config21 = config2.check();
        Assertions.assertNull(config21.getLogHandler());

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setLogHandlerClassName(MockJdbcEventLogHandler.class.getName());
        BeeDataSourceConfig config31 = config3.check();
        Assertions.assertNull(config31.getLogHandler());

        //test set a log collector to config object
        BeeJdbcEventLogManager logManager = new MockJdbcEventLogManager();
        config1.setLogManager(logManager);
        config11 = config1.check();
        Assertions.assertNotNull(config11.getLogHandler());

        config2.setLogManager(logManager);
        config21 = config2.check();
        Assertions.assertNotNull(config21.getLogHandler());

        config3.setLogManager(logManager);
        config31 = config3.check();
        Assertions.assertNotNull(config31.getLogHandler());
    }
}
