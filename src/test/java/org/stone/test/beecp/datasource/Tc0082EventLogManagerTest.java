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
import org.stone.beecp.BeeJdbcEventLog;
import org.stone.beecp.pool.DefaultJdbcEventLogHandler;
import org.stone.beecp.pool.DefaultJdbcEventLogManager;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beecp.BeeJdbcEventLog.Type_Connection_Get;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0082EventLogManagerTest {

    @Test
    public void testLogManagerSet() throws SQLException {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactory(new MockConnectionFactory());
        config1.setEventLogManager(new DefaultJdbcEventLogManager());
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
        }

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactory(new MockConnectionFactory());
        config2.setEventLogManagerClass(DefaultJdbcEventLogManager.class);
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
        }

        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactory(new MockConnectionFactory());
        config3.setEventLogManagerClassName(DefaultJdbcEventLogManager.class.getName());
        try (BeeDataSource ds = new BeeDataSource(config3)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
            Assertions.assertTrue(ds.getPoolMonitorVo().isEnabledEventLogManager());
            try (Connection ignore = ds.getConnection()) {
                List<BeeJdbcEventLog> logList = ds.getEventLog(Type_Connection_Get);
                Assertions.assertEquals(1, logList.size());
                BeeJdbcEventLog log = logList.get(0);
                Assertions.assertTrue(log.isSuccessful());
                Assertions.assertFalse(log.isException());
            }
            //disable log manager then retry to get a connection
            ds.enableEventLogManager(false);
            Assertions.assertFalse(ds.isEnabledEventLogManager());
            Assertions.assertFalse(ds.getPoolMonitorVo().isEnabledEventLogManager());
            Assertions.assertTrue(ds.getEventLog(Type_Connection_Get).isEmpty());//logs be cleared when disable log manager
            try (Connection ignore = ds.getConnection()) {
                Assertions.assertTrue(ds.getEventLog(Type_Connection_Get).isEmpty());//no logs generated when disable log manager
            }
            ds.enableEventLogManager(false);
            Assertions.assertTrue(ds.getEventLog(Type_Connection_Get).isEmpty());//no logs generated when disable log manager

            //re-enable log manager
            ds.enableEventLogManager(true);
            try (Connection ignore = ds.getConnection()) {
                Assertions.assertFalse(ds.getEventLog(Type_Connection_Get).isEmpty());//a new log should be generated
            }
            ds.enableEventLogManager(true);
            Assertions.assertFalse(ds.getEventLog(Type_Connection_Get).isEmpty());
        }
    }

    @Test
    public void testDsClearLog() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertFalse(ds.isEnabledEventLogManager());
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertTrue(ds.getEventLog(Type_Connection_Get).isEmpty());
                Assertions.assertTrue(ds.clearEventLog(Type_Connection_Get).isEmpty());
            }
        }

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setEventLogManager(new DefaultJdbcEventLogManager());
        config2.setConnectionFactoryClassName(connectionFactoryClassName);

        //2: clear test by type
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                ds.clearEventLog(Type_Connection_Get);
                Assertions.assertEquals(0, ds.getEventLog(Type_Connection_Get).size());
                ds.clearEventLog(BeeJdbcEventLog.Type_SQL_Execution);
                Assertions.assertEquals(0, ds.getEventLog(Type_Connection_Get).size());
            }
        }

        //3: clear all logs
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                Assertions.assertEquals(2, ds.getEventLog(Integer.MAX_VALUE).size());//any number not in[BeeJdbcEventLog.Type_Connection_Get,BeeJdbcEventLog.Type_SQL_Execution]

                ds.clearEventLog(Integer.MAX_VALUE);//any number not in[BeeJdbcEventLog.Type_Connection_Get,BeeJdbcEventLog.Type_SQL_Execution]

                Assertions.assertEquals(0, ds.getEventLog(Type_Connection_Get).size());
                Assertions.assertEquals(0, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                Assertions.assertEquals(0, ds.getEventLog(Integer.MAX_VALUE).size());
            }
        }
    }

    @Test
    public void testTimeoutClear() throws SQLException {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setEventLogManager(new DefaultJdbcEventLogManager());
        config1.setEventLogHandler(new DefaultJdbcEventLogHandler());
        config1.setSlowSQLThreshold(1L);
        config1.setSlowSQLThreshold(1L);
        config1.setEventLogHandledBySyncMode(false);//<-- async mode
        config1.setEventLogTimeout(1L);//1:milliseconds
        config1.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config1.setConnectionFactory(new MockConnectionFactory());
        Assertions.assertFalse(config1.isEventLogHandledBySyncMode());

        //1: clear type test(for sync mode)
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(0, ds.getEventLog(Integer.MAX_VALUE).size());
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setEventLogTimeout(1L);//1:milliseconds
        config2.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config2.setEventLogManager(new DefaultJdbcEventLogManager());
        config2.setConnectionFactory(new MockConnectionFactory());
        config2.setEventLogHandledBySyncMode(false);//async mode
        Assertions.assertFalse(config2.isEventLogHandledBySyncMode());
        //2: clear type test(for async mode)
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(0, ds.getEventLog(Integer.MAX_VALUE).size());
            }
        }
    }


    @Test
    public void testNotTimeout() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setEventLogTimeout(Long.MAX_VALUE);//1:milliseconds
        config1.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config1.setEventLogManager(new DefaultJdbcEventLogManager());
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        Assertions.assertTrue(config1.isEventLogHandledBySyncMode());
        //1: clear type test(for sync mode)
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(2, ds.getEventLog(Integer.MAX_VALUE).size());
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config1.setEventLogTimeout(Long.MAX_VALUE);//1:milliseconds
        config2.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config2.setEventLogManager(new DefaultJdbcEventLogManager());
        config2.setConnectionFactoryClassName(connectionFactoryClassName);
        config2.setEventLogHandledBySyncMode(false);//async mode
        Assertions.assertFalse(config2.isEventLogHandledBySyncMode());
        //2: clear type test(for async mode)
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(2, ds.getEventLog(Integer.MAX_VALUE).size());
            }
        }
    }

    @Test
    public void testSmallLogCache() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setEventLogTimeout(Long.MAX_VALUE);//1:milliseconds
        config1.setEventLogCacheSize(1);//test point
        config1.setEventLogManager(new DefaultJdbcEventLogManager());
        config1.setConnectionFactoryClassName(connectionFactoryClassName);

        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledEventLogManager());

            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
            }

            //twice
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Assertions.assertEquals(1, ds.getEventLog(Type_Connection_Get).size());
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
            }
        }
    }
}
