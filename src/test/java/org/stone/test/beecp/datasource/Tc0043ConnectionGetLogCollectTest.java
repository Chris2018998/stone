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
import org.stone.beecp.pool.DefaultJdbcLogCollector;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockCommonXaConnectionFactory;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0043ConnectionGetLogCollectTest {

    @Test
    public void testEnableAndDisableLogCollector() throws SQLException {
        String connectionFactoryClassName = "org.stone.test.beecp.objects.MockCommonConnectionFactory";

        //1：not configured log collector
        BeeDataSourceConfig config1 = createDefault();
        config1.setConnectionFactoryClassName(connectionFactoryClassName);
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertFalse(ds.isEnabledJdbcCallLogCollector());
            ds.enableJdbcCallLogCollector(true);
            Assertions.assertFalse(ds.isEnabledJdbcCallLogCollector());//no impact
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertTrue(ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection).isEmpty());
            }
        }

        //2: log collector created by log collector instance
        BeeDataSourceConfig config2 = createDefault();
        config2.setConnectionFactoryClassName(connectionFactoryClassName);
        config2.setJdbcCallLogCollector(new DefaultJdbcLogCollector());
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            //enabled log collector
            Assertions.assertTrue(ds.isEnabledJdbcCallLogCollector());
            try (Connection ignored = ds.getConnection()) {//getConnection call generate a log
                List<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection);
                Assertions.assertEquals(1, logList.size());
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertEquals(BeeJdbcCallLog.Type_Get_Connection, log.getType());
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

            //disable jdbc log collector
            ds.enableJdbcCallLogCollector(false);//just disable it and make log collector not work
            Assertions.assertFalse(ds.isEnabledJdbcCallLogCollector());
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertEquals(0, ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection).size());//connection get
            }

            //re-enable jdbc log collector
            ds.enableJdbcCallLogCollector(true);
            Assertions.assertTrue(ds.isEnabledJdbcCallLogCollector());
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertEquals(1, ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection).size());//connection get
            }
        }

        //3: log collector created by class
        BeeDataSourceConfig config3 = createDefault();
        config3.setConnectionFactoryClassName(connectionFactoryClassName);
        config3.setJdbcCallLogCollectorClass(DefaultJdbcLogCollector.class);
        try (BeeDataSource ds = new BeeDataSource(config3)) {
            Assertions.assertTrue(ds.isEnabledJdbcCallLogCollector());
            try (Connection ignored = ds.getConnection("test", "test")) {
                Assertions.assertEquals(1, ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection).size());
            }
        }

        //4: log collector created by class name
        BeeDataSourceConfig config4 = createDefault();
        config4.setConnectionFactoryClassName(connectionFactoryClassName);
        config4.setJdbcCallLogCollectorClassName(DefaultJdbcLogCollector.class.getName());
        try (BeeDataSource ds = new BeeDataSource(config4)) {
            Assertions.assertTrue(ds.isEnabledJdbcCallLogCollector());
            XAConnection xaCon = ds.getXAConnection();//log generation
            try (Connection ignored = xaCon.getConnection()) {//this call not be generated a log
                Assertions.assertEquals(1, ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection).size());
            }

            XAConnection xaCon2 = ds.getXAConnection();//log generation
            try (Connection ignored = xaCon2.getConnection()) {//this call not be generated a log
                Assertions.assertEquals(2, ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection).size());
            }
        }
    }

    @Test
    public void testExceptionLog() throws Exception {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcCallLogCollector(new DefaultJdbcLogCollector());
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        connectionFactory.setCreateException1(new SQLException("Failed to connect db"));
        config.setConnectionFactory(connectionFactory);

        //test1: Connection get test
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertTrue(ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection).isEmpty());

            //test1:getConnection()
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testExceptionLog]test failed");
            } catch (SQLException e) {
                List<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection);
                Assertions.assertTrue(logList != null && logList.size() == 1);
                for (BeeJdbcCallLog log : logList) {
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
                List<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection);
                Assertions.assertTrue(logList != null && logList.size() == 2);
                for (BeeJdbcCallLog log : logList) {
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
        config2.setJdbcCallLogCollector(new DefaultJdbcLogCollector());
        MockCommonXaConnectionFactory xaConnectionFactory = new MockCommonXaConnectionFactory();
        xaConnectionFactory.setCreateException1(new SQLException("Failed to connect db"));
        config2.setXaConnectionFactory(xaConnectionFactory);
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Collection<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection);
            Assertions.assertTrue(logList.isEmpty());

            //test2.1:getXAConnection()
            try {
                XAConnection ignored1 = ds.getXAConnection();
            } catch (SQLException e) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection);
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

            //test2.2: getXAConnection(String,String)
            try {
                XAConnection ignored2 = ds.getXAConnection("root", "test");
            } catch (SQLException e) {
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection);
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
        }
    }
}
