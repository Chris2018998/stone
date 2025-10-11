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
import org.stone.beecp.pool.DefaultJdbcEventLogManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0090JdbcLogManagerTest {

    @Test
    public void testLogManagerWorks() throws SQLException {
        //1: Not set Log Manager
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertFalse(ds.isEnabledJdbcEventLogManager());
            ds.enableJdbcEventLogManager(true);//no impact when not configure a log manager
            Assertions.assertFalse(ds.isEnabledJdbcEventLogManager());//still be false

            try (Connection ignore = ds.getConnection()) {
                Assertions.assertTrue(ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).isEmpty());//is empty when not configure a log manager
            }
        }

        //2：Set a log manager
        BeeDataSourceConfig config2 = createDefault();
        config2.setConnectionFactoryClassName(connectionFactoryClassName);
        config2.setLogManager(new DefaultJdbcEventLogManager());
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());//should be true
            try (Connection ignore = ds.getConnection()) {
                Assertions.assertFalse(ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).isEmpty());//is not empty when configure a log manager
            }

            //disable log manager then retry to get a connection
            ds.enableJdbcEventLogManager(false);
            Assertions.assertTrue(ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).isEmpty());//logs be cleared when disable log manager
            try (Connection ignore = ds.getConnection()) {
                Assertions.assertTrue(ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).isEmpty());//no logs generated when disable log manager
            }

            //re-enable log manager
            ds.enableJdbcEventLogManager(true);
            try (Connection ignore = ds.getConnection()) {
                Assertions.assertFalse(ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).isEmpty());//a new log should be generated
            }
        }
    }

    @Test
    public void testLogsDsClear() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setLogManager(new DefaultJdbcEventLogManager());
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        //1: clear type test
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                ds.clearJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get);
                Assertions.assertEquals(0, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                ds.clearJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution);
                Assertions.assertEquals(0, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
            }
        }

        //2: clear all test
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                Assertions.assertEquals(2, ds.getJdbcEventLog(Integer.MAX_VALUE).size());//any number not in[BeeJdbcEventLog.Type_Connection_Get,BeeJdbcEventLog.Type_SQL_Execution]

                ds.clearJdbcEventLog(Integer.MAX_VALUE);//any number not in[BeeJdbcEventLog.Type_Connection_Get,BeeJdbcEventLog.Type_SQL_Execution]

                Assertions.assertEquals(0, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                Assertions.assertEquals(0, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                Assertions.assertEquals(0, ds.getJdbcEventLog(Integer.MAX_VALUE).size());
            }
        }
    }

    @Test
    public void testTimeoutClear() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setLogTimeout(1L);//1:milliseconds
        config1.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config1.setLogManager(new DefaultJdbcEventLogManager());
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        Assertions.assertTrue(config1.isSlowLogHandledBySyncMode());
        //1: clear type test(for sync mode)
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(0, ds.getJdbcEventLog(Integer.MAX_VALUE).size());
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setLogTimeout(1L);//1:milliseconds
        config2.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config2.setLogManager(new DefaultJdbcEventLogManager());
        config2.setConnectionFactoryClassName(connectionFactoryClassName);
        config2.setSlowLogHandledBySyncMode(false);//async mode
        Assertions.assertFalse(config2.isSlowLogHandledBySyncMode());
        //2: clear type test(for async mode)
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(0, ds.getJdbcEventLog(Integer.MAX_VALUE).size());
            }
        }
    }

    @Test
    public void testNotTimeout() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setLogTimeout(Long.MAX_VALUE);//1:milliseconds
        config1.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config1.setLogManager(new DefaultJdbcEventLogManager());
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        Assertions.assertTrue(config1.isSlowLogHandledBySyncMode());
        //1: clear type test(for sync mode)
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(2, ds.getJdbcEventLog(Integer.MAX_VALUE).size());
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config1.setLogTimeout(Long.MAX_VALUE);//1:milliseconds
        config2.setIntervalToClearTimeoutEventLogs(500L);//500:milliseconds
        config2.setLogManager(new DefaultJdbcEventLogManager());
        config2.setConnectionFactoryClassName(connectionFactoryClassName);
        config2.setSlowLogHandledBySyncMode(false);//async mode
        Assertions.assertFalse(config2.isSlowLogHandledBySyncMode());
        //2: clear type test(for async mode)
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000L));//wait pool timer to clear timeout logs
                Assertions.assertEquals(2, ds.getJdbcEventLog(Integer.MAX_VALUE).size());
            }
        }
    }

    @Test
    public void testSmallLogCache() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.factory.MockConnectionFactory";
        BeeDataSourceConfig config1 = createDefault();
        config1.setLogTimeout(Long.MAX_VALUE);//1:milliseconds
        config1.setLogCacheSize(1);//test point
        config1.setLogManager(new DefaultJdbcEventLogManager());
        config1.setConnectionFactoryClassName(connectionFactoryClassName);

        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());

            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
            }

            //twice
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
                st.execute("select * from test_user");
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_SQL_Execution).size());
            }
        }
    }
}
