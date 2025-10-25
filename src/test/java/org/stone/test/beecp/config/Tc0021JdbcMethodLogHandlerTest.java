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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.jdbclog.MockJdbcMethodLogHandler;
import org.stone.test.beecp.objects.jdbclog.MockJdbcMethodLogHandler2;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0021JdbcMethodLogHandlerTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertNull(config.getMethodLogHandler());//default check
        config.setMethodLogHandler(new MockJdbcMethodLogHandler());
        Assertions.assertNotNull(config.getMethodLogHandler());//default check
        config.setMethodLogHandler(null);
        Assertions.assertNull(config.getMethodLogHandler());//default check

        Assertions.assertNull(config.getMethodLogHandlerClass());//default check
        config.setMethodLogHandlerClass(MockJdbcMethodLogHandler.class);
        Assertions.assertNotNull(config.getMethodLogHandlerClass());
        config.setMethodLogHandlerClass(null);
        Assertions.assertNull(config.getMethodLogHandlerClass());

        Assertions.assertNull(config.getMethodLogHandlerClassName());//default check
        config.setMethodLogHandlerClassName(MockJdbcMethodLogHandler.class.getName());
        Assertions.assertNotNull(config.getMethodLogHandlerClassName());
        config.setMethodLogHandlerClassName(null);
        Assertions.assertNull(config.getMethodLogHandlerClassName());
    }

    @Test
    public void testCheckFailed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setMethodLogHandlerClassName(MockJdbcMethodLogHandler2.class.getName());//class can not be
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
        config2.setMethodLogHandlerClassName(MockJdbcMethodLogHandler2.class.getName() + "_NOT");//class not found
        try {
            config2.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setMethodLogHandlerClassName("java.lang.String");
        try {
            config3.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
            Assertions.assertTrue(e.getCause().getMessage().contains("method log handler"));
        }


    }

    @Test
    public void testCheckPassed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();

        //1: instance
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(connectionFactory);
        MockJdbcMethodLogHandler handler = new MockJdbcMethodLogHandler();
        config1.setMethodLogHandler(handler);
        try {
            BeeDataSourceConfig checkedConfig = config1.check();
            Assertions.assertEquals(handler, checkedConfig.getMethodLogHandler());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setMethodLogHandlerClass(MockJdbcMethodLogHandler.class);
        try {
            config2.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(connectionFactory);
        config3.setMethodLogHandlerClassName(MockJdbcMethodLogHandler.class.getName());
        try {
            config3.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }
    }
}
