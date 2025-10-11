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
import org.stone.test.beecp.objects.jdbclog.MockJdbcEventLogManager;
import org.stone.tools.exception.BeanException;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0020JdbcCallLogManagerTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //jdbcCallLogCacheSize
        Assertions.assertEquals(1000, config.getLogCacheSize());//default check
        config.setLogCacheSize(500);
        Assertions.assertEquals(500, config.getLogCacheSize());
        try {
            config.setLogCacheSize(0);
            fail("[testConfigurationSet]Setting test failed on configuration item[log-cache-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'log-cache-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setLogCacheSize(-1);
            fail("[testConfigurationSet]Setting test failed on configuration item[log-cache-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'log-cache-size' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(500, config.getLogCacheSize());//not changed check

        //slowConnectionGetThreshold
        Assertions.assertEquals(30000L, config.getSlowConnectionGetThreshold());//default check
        config.setSlowConnectionGetThreshold(5000L);
        Assertions.assertEquals(5000L, config.getSlowConnectionGetThreshold());
        config.setSlowConnectionGetThreshold(0L);
        Assertions.assertEquals(0L, config.getSlowConnectionGetThreshold());
        try {
            config.setSlowConnectionGetThreshold(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[slow-connection-get-threshold]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'slow-connection-get-threshold' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getSlowConnectionGetThreshold());//not changed check

        //slowSQLExecutionThreshold
        Assertions.assertEquals(30000L, config.getSlowSQLExecutionThreshold());//default check
        config.setSlowSQLExecutionThreshold(5000L);
        Assertions.assertEquals(5000L, config.getSlowSQLExecutionThreshold());
        config.setSlowSQLExecutionThreshold(0L);
        Assertions.assertEquals(0L, config.getSlowSQLExecutionThreshold());
        try {
            config.setSlowSQLExecutionThreshold(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[slow-SQL-execution-threshold]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'slow-SQL-execution-threshold' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getSlowSQLExecutionThreshold());//not changed check

        //jdbcCallLogTimeout
        Assertions.assertEquals(180000L, config.getLogTimeout());//default check
        config.setLogTimeout(5000L);
        Assertions.assertEquals(5000L, config.getLogTimeout());
        try {
            config.setLogTimeout(0L);
            fail("[testConfigurationSet]Setting test failed on configuration item[log-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'log-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setLogTimeout(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[log-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'log-timeout' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getLogTimeout());//not changed check


        //jdbcCallLogTimeoutInterval
        Assertions.assertEquals(180000L, config.getIntervalToClearTimeoutEventLogs());//default check
        config.setIntervalToClearTimeoutEventLogs(5000L);
        Assertions.assertEquals(5000L, config.getIntervalToClearTimeoutEventLogs());
        try {
            config.setIntervalToClearTimeoutEventLogs(0L);
            fail("[testConfigurationSet]Setting test failed on configuration item[log-clear-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'log-clear-interval' must be greater than zero", e.getMessage());
        }
        try {
            config.setIntervalToClearTimeoutEventLogs(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[log-clear-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'log-clear-interval' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getIntervalToClearTimeoutEventLogs());//not changed check

        Assertions.assertNull(config.getLogManager());//default check
        config.setLogManager(new MockJdbcEventLogManager());
        Assertions.assertNotNull(config.getLogManager());
        config.setLogManager(null);
        Assertions.assertNull(config.getLogManager());

        Assertions.assertNull(config.getLogManagerClass());//default check
        config.setLogManagerClass(MockJdbcEventLogManager.class);
        Assertions.assertNotNull(config.getLogManagerClass());
        config.setLogManagerClass(null);
        Assertions.assertNull(config.getLogManagerClass());

        Assertions.assertNull(config.getLogManagerClassName());//default check
        config.setLogManagerClassName(MockJdbcEventLogManager.class.getName());
        Assertions.assertNotNull(config.getLogManagerClassName());
        config.setLogManagerClassName(null);
        Assertions.assertNull(config.getLogManagerClassName());
    }

    @Test
    public void testErrorClassName() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setLogManagerClassName("org.stone.test.beecp.objects.jdbclog.MockJdbcEventLogManager2");//class can not be instan
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
        config2.setLogManagerClassName("org.stone.test.beecp.objects.MockJdbcEventLogManager3");//class not found
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }
    }
}
