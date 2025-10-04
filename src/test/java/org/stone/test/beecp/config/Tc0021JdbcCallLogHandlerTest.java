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
import org.stone.beecp.BeeJdbcCallLogManager;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockJdbcCallLogManager;
import org.stone.test.beecp.objects.MockJdbcCallLogHandler;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0021JdbcCallLogHandlerTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertTrue(config.isSlowLogHandledBySyncMode());//default check
        config.setSlowLogHandledBySyncMode(false);
        Assertions.assertFalse(config.isSlowLogHandledBySyncMode());

        Assertions.assertNull(config.getSlowLogHandler());//default check
        config.setSlowLogHandler(new MockJdbcCallLogHandler());
        Assertions.assertNotNull(config.getSlowLogHandler());//default check
        config.setSlowLogHandler(null);
        Assertions.assertNull(config.getSlowLogHandler());//default check

        Assertions.assertNull(config.getSlowLogHandlerClass());//default check
        config.setSlowLogHandlerClass(MockJdbcCallLogHandler.class);
        Assertions.assertNotNull(config.getSlowLogHandlerClass());
        config.setSlowLogHandlerClass(null);
        Assertions.assertNull(config.getSlowLogHandlerClass());

        Assertions.assertNull(config.getSlowLogHandlerClassName());//default check
        config.setSlowLogHandlerClassName(MockJdbcCallLogHandler.class.getName());
        Assertions.assertNotNull(config.getSlowLogHandlerClassName());
        config.setSlowLogHandlerClassName(null);
        Assertions.assertNull(config.getSlowLogHandlerClassName());
    }

    @Test
    public void testWithoutLogCollector() throws Exception {
        BeeDataSourceConfig config1 = createEmpty();
        BeeConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        config1.setSlowLogHandler(new MockJdbcCallLogHandler());
        BeeDataSourceConfig config11 = config1.check();
        Assertions.assertNull(config11.getSlowLogHandler());

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setSlowLogHandlerClass(MockJdbcCallLogHandler.class);
        BeeDataSourceConfig config21 = config2.check();
        Assertions.assertNull(config21.getSlowLogHandler());

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setSlowLogHandlerClassName(MockJdbcCallLogHandler.class.getName());
        BeeDataSourceConfig config31 = config3.check();
        Assertions.assertNull(config31.getSlowLogHandler());

        //test set a log collector to config object
        BeeJdbcCallLogManager logCollector = new MockJdbcCallLogManager();
        config1.setJdbcCallLogManager(logCollector);
        config11 = config1.check();
        Assertions.assertNotNull(config11.getSlowLogHandler());

        config2.setJdbcCallLogManager(logCollector);
        config21 = config2.check();
        Assertions.assertNotNull(config21.getSlowLogHandler());

        config3.setJdbcCallLogManager(logCollector);
        config31 = config3.check();
        Assertions.assertNotNull(config31.getSlowLogHandler());
    }

    @Test
    public void testErrorClassName() throws Exception {
        BeeJdbcCallLogManager logCollector = new MockJdbcCallLogManager();
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setJdbcCallLogManager(logCollector);
        config1.setSlowLogHandlerClassName("org.stone.test.beecp.objects.MockJdbcCallLogHandler2");//class can not be
        try {
            config1.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause1 = e.getCause();
            Assertions.assertInstanceOf(BeanException.class, cause1);
            Assertions.assertInstanceOf(NoSuchMethodException.class, cause1.getCause());
        }

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setJdbcCallLogManager(logCollector);
        config2.setSlowLogHandlerClassName("org.stone.test.beecp.objects.MockJdbcCallLogHandler3");//class not found
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }


    }
}
