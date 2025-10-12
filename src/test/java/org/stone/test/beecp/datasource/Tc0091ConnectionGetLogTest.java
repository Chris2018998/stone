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
import org.stone.test.beecp.objects.factory.ExceptionConnectionFactory;
import org.stone.test.beecp.objects.factory.ExceptionXaConnectionFactory;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * @author Chris Liao
 */
public class Tc0091ConnectionGetLogTest {

    @Test
    public void testEnableAndDisableLogManager() throws SQLException {
        String connectionFactoryClassName = MockConnectionFactory.class.getName();

        //1：not configured log manager
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertFalse(ds.isEnabledJdbcEventLogManager());
            ds.enableJdbcEventLogManager(true);
            Assertions.assertFalse(ds.isEnabledJdbcEventLogManager());//no impact
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertTrue(ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).isEmpty());
            }
        }

        //2: log manager created by log manager instance
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactoryClassName(connectionFactoryClassName);
        config2.setLogManager(new DefaultJdbcEventLogManager());
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            //enabled log manager
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection ignored = ds.getConnection()) {//getConnection call generate a log
                List<BeeJdbcEventLog> logList = ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get);
                Assertions.assertEquals(1, logList.size());
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertEquals(BeeJdbcEventLog.Type_Connection_Get, log.getType());
                    Assertions.assertNotNull(log.getId());
                    Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                    Assertions.assertNull(log.getParameters());//parameters to call method

                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNull(log.getFailCause());
                    Assertions.assertNotNull(log.getResultObject());
                    Assertions.assertInstanceOf(Connection.class, log.getResultObject());
                }
            }

            //disable jdbc log manager
            ds.enableJdbcEventLogManager(false);//just disable it and make log manager not work
            Assertions.assertFalse(ds.isEnabledJdbcEventLogManager());
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertEquals(0, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());//connection get
            }
            XAConnection xaCon = ds.getXAConnection();
            try (Connection ignored = xaCon.getConnection()) {
                Assertions.assertEquals(0, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());//connection get
            }

            //re-enable jdbc log manager
            ds.enableJdbcEventLogManager(true);
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());//connection get
            }
        }

        //3: log manager created by class
        BeeDataSourceConfig config3 = new BeeDataSourceConfig();
        config3.setConnectionFactoryClassName(connectionFactoryClassName);
        config3.setLogManagerClass(DefaultJdbcEventLogManager.class);
        try (BeeDataSource ds = new BeeDataSource(config3)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            try (Connection ignored = ds.getConnection("test", "test")) {
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
            }
        }

        //4: log manager created by class name
        BeeDataSourceConfig config4 = new BeeDataSourceConfig();
        config4.setConnectionFactoryClassName(connectionFactoryClassName);
        config4.setLogManagerClassName(DefaultJdbcEventLogManager.class.getName());
        try (BeeDataSource ds = new BeeDataSource(config4)) {
            Assertions.assertTrue(ds.isEnabledJdbcEventLogManager());
            XAConnection xaCon = ds.getXAConnection();//log generation
            try (Connection ignored = xaCon.getConnection()) {//this call not be generated a log
                Assertions.assertEquals(1, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
            }

            XAConnection xaCon2 = ds.getXAConnection();//log generation
            try (Connection ignored = xaCon2.getConnection()) {//this call not be generated a log
                Assertions.assertEquals(2, ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).size());
            }
        }
    }

    @Test
    public void testExceptionLog() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setLogManager(new DefaultJdbcEventLogManager());
        ExceptionConnectionFactory connectionFactory = new ExceptionConnectionFactory();
        connectionFactory.setFailCause(new SQLException("Failed to connect db"));
        config.setConnectionFactory(connectionFactory);

        //test1: Connection get test
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertTrue(ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get).isEmpty());

            //test1:getConnection()
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testExceptionLog]test failed");
            } catch (SQLException e) {
                List<BeeJdbcEventLog> logList = ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get);
                Assertions.assertTrue(logList != null && logList.size() == 1);
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());//fail exception
                }
            }

            //test1.2: getConnection(String,String)
            try (Connection ignored = ds.getConnection("root", "test")) {
                Assertions.fail("[testExceptionLog]test failed");
            } catch (SQLException e) {
                List<BeeJdbcEventLog> logList = ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get);
                Assertions.assertTrue(logList != null && logList.size() == 2);
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());//fail exception
                }
            }
        }

        //2: XAConnection get test
        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setLogManager(new DefaultJdbcEventLogManager());
        ExceptionXaConnectionFactory xaConnectionFactory = new ExceptionXaConnectionFactory();
        xaConnectionFactory.setFailCause(new SQLException("Failed to connect db"));
        config2.setXaConnectionFactory(xaConnectionFactory);
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Collection<BeeJdbcEventLog> logList = ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get);
            Assertions.assertTrue(logList.isEmpty());

            //test2.1:getXAConnection()
            try {
                XAConnection ignored1 = ds.getXAConnection();
            } catch (SQLException e) {
                logList = ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get);
                Assertions.assertTrue(logList != null && logList.size() == 1);
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getXAConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());

                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());
                }
            }

            //test2.2: getXAConnection(String,String)
            try {
                XAConnection ignored2 = ds.getXAConnection("root", "test");
            } catch (SQLException e) {
                logList = ds.getJdbcEventLog(BeeJdbcEventLog.Type_Connection_Get);
                Assertions.assertTrue(logList != null && logList.size() == 2);
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertEquals("FastConnectionPool4L.getXAConnection()", log.getMethod());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNull(log.getResultObject());
                    Assertions.assertNotNull(log.getFailCause());
                }
            }
        }
    }
}
