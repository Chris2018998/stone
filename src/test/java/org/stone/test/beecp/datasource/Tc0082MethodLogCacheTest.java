/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.listener.MockMethodExecutionListener1;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.BeeMethodExecutionLog.Type_All;
import static org.stone.beecp.BeeMethodExecutionLog.Type_Pool_Log;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0082MethodLogCacheTest {

    @Test
    public void testCacheEnableAndDisable() throws SQLException {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(new MockConnectionFactory());
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertFalse(ds.getPoolMonitorVo().isEnabledMethodExecutionLogCache());
            try (Connection ignore = ds.getConnection()) {
                ds.setMethodExecutionListener(new MockMethodExecutionListener1());
                Assertions.assertTrue(ds.getLogs(Type_Pool_Log).isEmpty());
                ds.clearLogs(Type_All);
                Assertions.assertTrue(ds.getLogs(Type_All).isEmpty());
                ds.enableLogCache(true);
                ds.enableLogCache(true);
                Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledMethodExecutionLogCache());
                ds.enableLogCache(false);
                ds.enableLogCache(false);
                Assertions.assertFalse(ds.cancelStatement("Teat"));
                ds.setMethodExecutionListener(null);
            }
        }
    }

    @Test
    public void testSmallLogCache() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setEnableMethodExecutionLogCache(true);
        config1.setMethodExecutionLogTimeout(Long.MAX_VALUE);//1:milliseconds
        config1.setMethodExecutionLogCacheSize(1);//test point
        config1.setConnectionFactoryClassName(connectionFactoryClassName);

        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledMethodExecutionLogCache());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Assertions.assertEquals(1, ds.getLogs(Type_Pool_Log).size());
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getLogs(BeeMethodExecutionLog.Type_Statement_Log).size());
            }

            //twice
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Assertions.assertEquals(1, ds.getLogs(Type_Pool_Log).size());
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getLogs(BeeMethodExecutionLog.Type_Statement_Log).size());
            }
        }
    }

    @Test
    public void testTimeoutClear() throws SQLException {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setEnableMethodExecutionLogCache(true);
        config.setMethodExecutionListener(new MockMethodExecutionListener1());
        config.setSlowSQLThreshold(1L);
        config.setSlowConnectionThreshold(1L);

        config.setMethodExecutionLogTimeout(1L);//1:milliseconds
        config.setIntervalOfClearTimeoutExecutionLogs(100L);//500:milliseconds

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setParkNanos(200L);
        connectionProperties.parkWhenCallMethod("executeUpdate");
        connectionProperties.throwsExceptionWhenCallMethod("execute");
        SQLException failException = new SQLException("NetWork failed");
        connectionProperties.setMockException1(failException);

        MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
        connectionFactory.setNeedPark(true);
        connectionFactory.setParkNanos(TimeUnit.MILLISECONDS.toNanos(200L));
        config.setConnectionFactory(connectionFactory);

        //1: clear type test(for sync mode)
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledMethodExecutionLogCache());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.executeUpdate("update test_user set name ='chris' where id=1");
                try {
                    st.execute("select * from test_user");
                    Assertions.fail("[testTimeoutClear]test failed");
                } catch (SQLException e) {
                    Assertions.assertEquals(failException, e);
                }
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(0, ds.getLogs(Type_All).size());
            }
        }
    }
}