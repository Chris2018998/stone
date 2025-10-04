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
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockJdbcCallLogManager;
import org.stone.tools.exception.BeanException;

import java.security.InvalidParameterException;

import static java.util.concurrent.TimeUnit.MINUTES;
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
        Assertions.assertEquals(1000, config.getJdbcCallLogCacheSize());//default check
        config.setJdbcCallLogCacheSize(500);
        Assertions.assertEquals(500, config.getJdbcCallLogCacheSize());
        try {
            config.setJdbcCallLogCacheSize(0);
            fail("[testConfigurationSet]Setting test failed on configuration item[jdbc-call-log-cache-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'jdbc-call-log-cache-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setJdbcCallLogCacheSize(-1);
            fail("[testConfigurationSet]Setting test failed on configuration item[jdbc-call-log-cache-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'jdbc-call-log-cache-size' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(500, config.getJdbcCallLogCacheSize());//not changed check

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
        Assertions.assertEquals(MINUTES.toMillis(3L), config.getJdbcCallLogTimeout());//default check
        config.setJdbcCallLogTimeout(5000L);
        Assertions.assertEquals(5000L, config.getJdbcCallLogTimeout());
        try {
            config.setJdbcCallLogTimeout(0L);
            fail("[testConfigurationSet]Setting test failed on configuration item[jdbc-call-log-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'jdbc-call-log-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setJdbcCallLogTimeout(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[jdbc-call-log-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'jdbc-call-log-timeout' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getJdbcCallLogTimeout());//not changed check


        //jdbcCallLogTimeoutInterval
        Assertions.assertEquals(MINUTES.toMillis(3L), config.getJdbcCallLogClearInterval());//default check
        config.setJdbcCallLogClearInterval(5000L);
        Assertions.assertEquals(5000L, config.getJdbcCallLogClearInterval());
        try {
            config.setJdbcCallLogClearInterval(0L);
            fail("[testConfigurationSet]Setting test failed on configuration item[jdbc-call-log-clear-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'jdbc-call-log-clear-interval' must be greater than zero", e.getMessage());
        }
        try {
            config.setJdbcCallLogClearInterval(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[jdbc-call-log-clear-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'jdbc-call-log-clear-interval' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getJdbcCallLogClearInterval());//not changed check

        Assertions.assertNull(config.getJdbcCallLogManager());//default check
        config.setJdbcCallLogManager(new MockJdbcCallLogManager());
        Assertions.assertNotNull(config.getJdbcCallLogManager());
        config.setJdbcCallLogManager(null);
        Assertions.assertNull(config.getJdbcCallLogManager());

        Assertions.assertNull(config.getJdbcCallLogManagerClass());//default check
        config.setJdbcCallLogManagerClass(MockJdbcCallLogManager.class);
        Assertions.assertNotNull(config.getJdbcCallLogManagerClass());
        config.setJdbcCallLogManagerClass(null);
        Assertions.assertNull(config.getJdbcCallLogManagerClass());

        Assertions.assertNull(config.getJdbcCallLogManagerClassName());//default check
        config.setJdbcCallLogManagerClassName(MockJdbcCallLogManager.class.getName());
        Assertions.assertNotNull(config.getJdbcCallLogManagerClassName());
        config.setJdbcCallLogManagerClassName(null);
        Assertions.assertNull(config.getJdbcCallLogManagerClassName());
    }

    @Test
    public void testErrorClassName() throws Exception {
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setJdbcCallLogManagerClassName("org.stone.test.beecp.objects.MockJdbcCallLogManager2");//class can not be instan
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
        config2.setJdbcCallLogManagerClassName("org.stone.test.beecp.objects.MockJdbcCallLogManager3");//class not found
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }
    }
}
