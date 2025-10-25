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
        Assertions.assertNull(config.getMethodExecutionListener());//default check
        config.setMethodExecutionListener(new MockJdbcMethodLogHandler());
        Assertions.assertNotNull(config.getMethodExecutionListener());//default check
        config.setMethodExecutionListener(null);
        Assertions.assertNull(config.getMethodExecutionListener());//default check

        Assertions.assertNull(config.getMethodExecutionListenerClass());//default check
        config.setMethodExecutionListenerClass(MockJdbcMethodLogHandler.class);
        Assertions.assertNotNull(config.getMethodExecutionListenerClass());
        config.setMethodExecutionListenerClass(null);
        Assertions.assertNull(config.getMethodExecutionListenerClass());

        Assertions.assertNull(config.getMethodExecutionListenerClassName());//default check
        config.setMethodExecutionListenerClassName(MockJdbcMethodLogHandler.class.getName());
        Assertions.assertNotNull(config.getMethodExecutionListenerClassName());
        config.setMethodExecutionListenerClassName(null);
        Assertions.assertNull(config.getMethodExecutionListenerClassName());
    }

    @Test
    public void testCheckFailed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setMethodExecutionListenerClassName(MockJdbcMethodLogHandler2.class.getName());//class can not be
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
        config2.setMethodExecutionListenerClassName(MockJdbcMethodLogHandler2.class.getName() + "_NOT");//class not found
        try {
            config2.check();
            Assertions.fail("[testCheckFailed]Test failed");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setMethodExecutionListenerClassName("java.lang.String");
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
        config1.setMethodExecutionListener(handler);
        try {
            BeeDataSourceConfig checkedConfig = config1.check();
            Assertions.assertEquals(handler, checkedConfig.getMethodExecutionListener());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setMethodExecutionListenerClass(MockJdbcMethodLogHandler.class);
        try {
            config2.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(connectionFactory);
        config3.setMethodExecutionListenerClassName(MockJdbcMethodLogHandler.class.getName());
        try {
            config3.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }
    }
}
