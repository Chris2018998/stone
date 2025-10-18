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
import org.stone.test.beecp.objects.jdbclog.MockJdbcEventLogManager2;
import org.stone.tools.exception.BeanException;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0020JdbcEventLogManagerTest {

    @Test
    public void testSetAndGet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //jdbcCallLogCacheSize
        Assertions.assertEquals(1000, config.getEventLogCacheSize());//default check
        config.setEventLogCacheSize(500);
        Assertions.assertEquals(500, config.getEventLogCacheSize());
        try {
            config.setEventLogCacheSize(0);
            fail("[testSetAndGet]Setting test failed on configuration item[log-cache-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'event-log-cache-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setEventLogCacheSize(-1);
            fail("[testSetAndGet]Setting test failed on configuration item[log-cache-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'event-log-cache-size' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(500, config.getEventLogCacheSize());//not changed check

        //slowConnectionGetThreshold
        Assertions.assertEquals(30000L, config.getSlowConnectionThreshold());//default check
        config.setSlowConnectionThreshold(5000L);
        Assertions.assertEquals(5000L, config.getSlowConnectionThreshold());
        config.setSlowConnectionThreshold(0L);
        Assertions.assertEquals(0L, config.getSlowConnectionThreshold());
        try {
            config.setSlowConnectionThreshold(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[slow-connection-get-threshold]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'slow-connection-threshold' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getSlowConnectionThreshold());//not changed check

        //slowSQLExecutionThreshold
        Assertions.assertEquals(30000L, config.getSlowSQLThreshold());//default check
        config.setSlowSQLThreshold(5000L);
        Assertions.assertEquals(5000L, config.getSlowSQLThreshold());
        config.setSlowSQLThreshold(0L);
        Assertions.assertEquals(0L, config.getSlowSQLThreshold());
        try {
            config.setSlowSQLThreshold(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[slow-SQL-execution-threshold]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'slow-SQL-threshold' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getSlowSQLThreshold());//not changed check

        //jdbcCallLogTimeout
        Assertions.assertEquals(180000L, config.getEventLogTimeout());//default check
        config.setEventLogTimeout(5000L);
        Assertions.assertEquals(5000L, config.getEventLogTimeout());
        try {
            config.setEventLogTimeout(0L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'event-log-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setEventLogTimeout(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'event-log-timeout' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getEventLogTimeout());//not changed check


        //jdbcCallLogTimeoutInterval
        Assertions.assertEquals(180000L, config.getIntervalToClearTimeoutEventLogs());//default check
        config.setIntervalToClearTimeoutEventLogs(5000L);
        Assertions.assertEquals(5000L, config.getIntervalToClearTimeoutEventLogs());
        try {
            config.setIntervalToClearTimeoutEventLogs(0L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-clear-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'interval-to-clear-timeout-event-logs' must be greater than zero", e.getMessage());
        }
        try {
            config.setIntervalToClearTimeoutEventLogs(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[log-clear-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'interval-to-clear-timeout-event-logs' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getIntervalToClearTimeoutEventLogs());//not changed check

        Assertions.assertNull(config.getEventLogManager());//default check
        config.setEventLogManager(new MockJdbcEventLogManager());
        Assertions.assertNotNull(config.getEventLogManager());
        config.setEventLogManager(null);
        Assertions.assertNull(config.getEventLogManager());

        Assertions.assertNull(config.getEventlogManagerClass());//default check
        config.setEventLogManagerClass(MockJdbcEventLogManager.class);
        Assertions.assertNotNull(config.getEventlogManagerClass());
        config.setEventLogManagerClass(null);
        Assertions.assertNull(config.getEventlogManagerClass());

        Assertions.assertNull(config.getEventLogManagerClassName());//default check
        config.setEventLogManagerClassName(MockJdbcEventLogManager.class.getName());
        Assertions.assertNotNull(config.getEventLogManagerClassName());
        config.setEventLogManagerClassName(null);
        Assertions.assertNull(config.getEventLogManagerClassName());
    }

    @Test
    public void testCheckFailed() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setEventLogManagerClassName(MockJdbcEventLogManager2.class.getName());//class can not be instantiated
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
        config2.setEventLogManagerClassName(MockJdbcEventLogManager2.class.getName() + "_NOT");//class not found
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

        //1: instance
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(connectionFactory);
        MockJdbcEventLogManager manager = new MockJdbcEventLogManager();
        config1.setEventLogManager(manager);
        try {
            BeeDataSourceConfig checkedConfig = config1.check();
            Assertions.assertEquals(manager, checkedConfig.getEventLogManager());
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //2: class
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(connectionFactory);
        config2.setEventLogManagerClass(MockJdbcEventLogManager.class);
        try {
            config2.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }

        //3: class name
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(connectionFactory);
        config3.setEventLogManagerClassName(MockJdbcEventLogManager.class.getName());
        try {
            config3.check();
        } catch (BeeDataSourceConfigException e) {
            Assertions.fail("[testCheckPassed]Test failed");
        }
    }
}
