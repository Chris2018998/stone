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
import org.stone.beecp.BeeJdbcCallLog;
import org.stone.beecp.pool.JdbcCallLogCollectorImpl;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockCommonXaConnectionFactory;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Chris Liao
 */
public class Tc0043ConnectionLogCollectTest {

    @Test
    public void testGetConnection() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcCallLogCollector(new JdbcCallLogCollectorImpl());
        config.setConnectionFactory(new MockCommonConnectionFactory());
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Collection<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
            Assertions.assertTrue(logList.isEmpty());

            //test1:getConnection()
            try (Connection ignored = ds.getConnection()) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 1);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNotNull(log.getResultObject());
                }
            }

            //test2: getConnection(String,String)
            try (Connection ignored = ds.getConnection("root", "test")) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 2);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNotNull(log.getResultObject());
                }
            }

            ds.clearJdbcCallLog(0L);
            logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
            Assertions.assertTrue(logList.isEmpty());
        }
    }

    @Test
    public void testExceptionOnGetConnection() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcCallLogCollector(new JdbcCallLogCollectorImpl());
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        connectionFactory.setCreateException1(new SQLException("Failed to connect db"));
        config.setConnectionFactory(connectionFactory);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Collection<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
            Assertions.assertTrue(logList.isEmpty());

            //test1:getConnection()
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail();
            } catch (SQLException e) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 1);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());

                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());
                }
            }

            //test2: getConnection(String,String)
            try (Connection ignored = ds.getConnection("root", "test")) {
                Assertions.fail();
            } catch (SQLException e) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 2);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());
                }
            }

            ds.clearJdbcCallLog(0L);
            logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
            Assertions.assertTrue(logList.isEmpty());
        }
    }


    @Test
    public void testGetXAConnection() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcCallLogCollector(new JdbcCallLogCollectorImpl());
        config.setXaConnectionFactory(new MockCommonXaConnectionFactory());
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Collection<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
            Assertions.assertTrue(logList.isEmpty());

            //test1:getXAConnection()
            XAConnection ignored1 = ds.getXAConnection();
            try (Connection ignored11 = ignored1.getConnection()) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 1);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getXAConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNotNull(log.getResultObject());
                }
            }

            //test2: getXAConnection(String,String)
            XAConnection ignored2 = ds.getXAConnection("root", "test");
            try (Connection ignored21 = ignored2.getConnection()) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 2);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getXAConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNotNull(log.getResultObject());
                }

                ds.clearJdbcCallLog(0L);
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList.isEmpty());
            }
        }
    }

    @Test
    public void testExceptionOnGetXAConnection() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcCallLogCollector(new JdbcCallLogCollectorImpl());
        MockCommonXaConnectionFactory connectionFactory = new MockCommonXaConnectionFactory();
        connectionFactory.setCreateException1(new SQLException("Failed to connect db"));
        config.setXaConnectionFactory(connectionFactory);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Collection<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
            Assertions.assertTrue(logList.isEmpty());

            //test1:getXAConnection()
            try {
                XAConnection ignored1 = ds.getXAConnection();
                try (Connection ignored11 = ignored1.getConnection()) {
                    Assertions.fail();
                }
            } catch (SQLException e) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 1);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getXAConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());

                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());
                }
            }

            //test2: getXAConnection(String,String)
            try {
                XAConnection ignored2 = ds.getXAConnection("root", "test");
                try (Connection ignored21 = ignored2.getConnection()) {
                    Assertions.fail();
                }
            } catch (SQLException e) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
                Assertions.assertTrue(logList != null && logList.size() == 2);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getXAConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());
                }
            }

            ds.clearJdbcCallLog(0L);
            logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_GetConnection);
            Assertions.assertTrue(logList.isEmpty());
        }
    }
}
