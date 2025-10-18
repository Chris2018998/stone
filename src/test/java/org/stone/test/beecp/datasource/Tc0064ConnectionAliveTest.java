/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;
import org.stone.test.base.LogCollector;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0064ConnectionAliveTest {
    @Test
    public void testAliveTestFalseByIsValid() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintRuntimeLogs(true);
        config.setAliveAssumeTime(0L);
        config.setMaxWait(1L);

        MockConnectionProperties propertiesSet = new MockConnectionProperties();
        MockConnectionFactory factory = new MockConnectionFactory(propertiesSet);
        config.setConnectionFactory(factory);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            propertiesSet.setValid(false);
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("testAliveTestFalseByIsValid");
            } catch (ConnectionGetTimeoutException e) {
                Assertions.assertEquals("Waited timeout for a released connection", e.getMessage());
            }
        }
    }

    @Test
    public void testValidExceptionByIsValid() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        config1.setMaxActive(1);
        config1.setAliveAssumeTime(0L);
        config1.setMaxWait(1L);
        config1.setForceRecycleBorrowedOnClose(true);
        MockConnectionProperties propertiesSet = new MockConnectionProperties();
        MockConnectionFactory factory = new MockConnectionFactory(propertiesSet);
        config1.setConnectionFactory(factory);
        config1.setPrintRuntimeLogs(true);

        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            propertiesSet.setMockException1(new SQLException());
            propertiesSet.throwsExceptionWhenCallMethod("isValid");

            LogCollector logCollector = LogCollector.startLogCollector();
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("testValidExceptionByIsValid");
            } catch (ConnectionGetTimeoutException e) {
                String logs = logCollector.endLogCollector();
                Assertions.assertTrue(logs.contains("alive test failed on a borrowed connection"));
                Assertions.assertEquals("Waited timeout for a released connection", e.getMessage());
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setMaxActive(1);
        config2.setAliveAssumeTime(0L);
        config2.setMaxWait(1L);
        config2.setForceRecycleBorrowedOnClose(true);
        MockConnectionProperties propertiesSet2 = new MockConnectionProperties();
        MockConnectionFactory factory2 = new MockConnectionFactory(propertiesSet2);
        config2.setConnectionFactory(factory2);
        config2.setPrintRuntimeLogs(false);

        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            propertiesSet2.setMockException1(new SQLException());
            propertiesSet2.throwsExceptionWhenCallMethod("isValid");

            LogCollector logCollector = LogCollector.startLogCollector();
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("testValidExceptionByIsValid");
            } catch (ConnectionGetTimeoutException e) {
                String logs = logCollector.endLogCollector();
                Assertions.assertFalse(logs.contains("alive test failed on a borrowed connection"));
                Assertions.assertEquals("Waited timeout for a released connection", e.getMessage());
            }
        }
    }

    @Test
    public void testAliveTestPassByStatement() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        config1.setMaxActive(1);
        config1.setAliveAssumeTime(0L);
        config1.setMaxWait(1L);
        config1.setDefaultAutoCommit(true);
        MockConnectionProperties propertiesSet = new MockConnectionProperties();
        propertiesSet.setValid(false);
        MockConnectionFactory factory = new MockConnectionFactory(propertiesSet);
        config1.setConnectionFactory(factory);

        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertEquals(1, ds.getPoolMonitorVo().getIdleSize());
            try (Connection con1 = ds.getConnection()) {//success test
                Assertions.assertNotNull(con1);
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setMaxActive(1);
        config2.setAliveAssumeTime(0L);
        config2.setMaxWait(1L);
        config2.setDefaultAutoCommit(false);
        config2.setPrintRuntimeLogs(true);
        MockConnectionProperties propertiesSet2 = new MockConnectionProperties();
        propertiesSet2.setValid(false);
        MockConnectionFactory factory2 = new MockConnectionFactory(propertiesSet2);
        config2.setConnectionFactory(factory2);

        try (BeeDataSource ds = new BeeDataSource(config1)) {
            try (Connection con2 = ds.getConnection()) {//success test
                Assertions.assertNotNull(con2);
            }
        }

        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setMaxActive(1);
        config3.setAliveAssumeTime(0L);
        config3.setMaxWait(1L);
        config3.setDefaultAutoCommit(true);
        config3.setPrintRuntimeLogs(false);
        MockConnectionProperties propertiesSet3 = new MockConnectionProperties();
        propertiesSet3.setValid(false);
        MockConnectionFactory factory3 = new MockConnectionFactory(propertiesSet3);
        config3.setConnectionFactory(factory3);
        FastConnectionPool pool3 = new FastConnectionPool();
        pool3.start(config3);
        Assertions.assertEquals(1, pool3.getIdleSize());
        Connection con3 = null;
        try {//success test
            propertiesSet3.throwsExceptionWhenCallMethod("setQueryTimeout");
            propertiesSet3.setMockException1(new SQLException("setQueryTimeout failed"));
            LogCollector logCollector = LogCollector.startLogCollector();
            con3 = pool3.getConnection();
            String logs = logCollector.endLogCollector();
            Assertions.assertFalse(logs.contains("failed to set query timeout value on statement of a borrowed connection"));
            Assertions.assertNotNull(con3);
        } finally {
            TestUtil.oclose(con3);
            pool3.close();
        }

        BeeDataSourceConfig config4 = createDefault();
        config4.setInitialSize(1);
        config4.setMaxActive(1);
        config4.setAliveAssumeTime(0L);
        config4.setMaxWait(1L);
        config4.setDefaultAutoCommit(true);
        config4.setPrintRuntimeLogs(true);
        MockConnectionProperties propertiesSet4 = new MockConnectionProperties();
        propertiesSet4.setValid(false);
        MockConnectionFactory factory4 = new MockConnectionFactory(propertiesSet4);
        config4.setConnectionFactory(factory4);
        FastConnectionPool pool4 = new FastConnectionPool();
        pool4.start(config4);
        Assertions.assertEquals(1, pool4.getIdleSize());
        Connection con4 = null;
        try {//success test
            propertiesSet4.throwsExceptionWhenCallMethod("setQueryTimeout");
            propertiesSet4.setMockException1(new SQLException("setQueryTimeout failed"));
            LogCollector logCollector = LogCollector.startLogCollector();
            con4 = pool4.getConnection();
            String logs = logCollector.endLogCollector();
            Assertions.assertTrue(logs.contains("failed to set query timeout value on statement of a borrowed connection"));
            Assertions.assertNotNull(con4);
        } finally {
            TestUtil.oclose(con4);
            pool4.close();
        }

        BeeDataSourceConfig config5 = createDefault();
        config5.setInitialSize(1);
        config5.setMaxActive(1);
        config5.setAliveAssumeTime(0L);
        config5.setMaxWait(1L);
        config5.setDefaultAutoCommit(true);
        config5.setPrintRuntimeLogs(true);
        MockConnectionProperties propertiesSet5 = new MockConnectionProperties();
        propertiesSet5.setValid(false);
        MockConnectionFactory factory5 = new MockConnectionFactory(propertiesSet5);
        config5.setConnectionFactory(factory5);
        FastConnectionPool pool5 = new FastConnectionPool();
        propertiesSet5.throwsExceptionWhenCallMethod("setQueryTimeout");
        propertiesSet5.setMockException1(new SQLException("setQueryTimeout failed"));

        pool5.start(config5);
        Assertions.assertEquals(1, pool5.getIdleSize());
        Connection con5 = null;
        try {//success test
            LogCollector logCollector = LogCollector.startLogCollector();
            con5 = pool5.getConnection();
            String logs = logCollector.endLogCollector();
            Assertions.assertFalse(logs.contains("failed to set query timeout value on statement of a borrowed connection"));
            Assertions.assertNotNull(con5);
        } finally {
            TestUtil.oclose(con5);
            pool5.close();
        }
    }

    @Test
    public void testAliveTestFalseBySqlStatement() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        config1.setMaxActive(1);
        config1.setAliveAssumeTime(0L);
        config1.setMaxWait(1L);
        config1.setPrintRuntimeLogs(true);

        MockConnectionProperties propertiesSet1 = new MockConnectionProperties();
        propertiesSet1.setValid(false);
        MockConnectionFactory factory1 = new MockConnectionFactory(propertiesSet1);
        config1.setConnectionFactory(factory1);
        FastConnectionPool pool1 = new FastConnectionPool();

        pool1.start(config1);
        Assertions.assertEquals(1, pool1.getIdleSize());
        Connection con = null;
        try {//
            propertiesSet1.setMockException1(new SQLException("execute fail"));
            propertiesSet1.throwsExceptionWhenCallMethod("execute");
            LogCollector logCollector = LogCollector.startLogCollector();
            con = pool1.getConnection();
            String logs = logCollector.endLogCollector();
            Assertions.assertTrue(logs.contains("connection alive test failed with sql,pool will abandon it"));
            Assertions.fail("testAliveTestFalseBySqlStatement");
        } catch (ConnectionGetTimeoutException e) {
            Assertions.assertEquals("Waited timeout for a released connection", e.getMessage());
        } finally {
            TestUtil.oclose(con);
            pool1.close();
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setMaxActive(1);
        config2.setAliveAssumeTime(0L);
        config2.setMaxWait(1L);
        config2.setPrintRuntimeLogs(false);
        MockConnectionProperties propertiesSet2 = new MockConnectionProperties();
        propertiesSet2.setValid(false);
        MockConnectionFactory factory2 = new MockConnectionFactory(propertiesSet2);
        config2.setConnectionFactory(factory2);
        FastConnectionPool pool2 = new FastConnectionPool();

        pool2.start(config2);
        Assertions.assertEquals(1, pool2.getIdleSize());
        Connection con2 = null;
        try {//
            propertiesSet2.setMockException1(new SQLException("execute fail"));
            propertiesSet2.throwsExceptionWhenCallMethod("execute");
            LogCollector logCollector = LogCollector.startLogCollector();
            con2 = pool2.getConnection();
            String logs = logCollector.endLogCollector();
            Assertions.assertFalse(logs.contains("connection alive test failed with sql,pool will abandon it"));
            Assertions.fail("testAliveTestFalseBySqlStatement");
        } catch (ConnectionGetTimeoutException e) {
            Assertions.assertEquals("Waited timeout for a released connection", e.getMessage());
        } finally {
            TestUtil.oclose(con2);
            pool2.close();
        }


        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setMaxActive(1);
        config3.setAliveAssumeTime(0L);
        config3.setMaxWait(1L);
        config3.setPrintRuntimeLogs(false);
        MockConnectionProperties propertiesSet3 = new MockConnectionProperties();
        propertiesSet3.setValid(false);
        MockConnectionFactory factory3 = new MockConnectionFactory(propertiesSet3);
        config3.setConnectionFactory(factory3);
        FastConnectionPool pool3 = new FastConnectionPool();

        pool3.start(config3);
        Assertions.assertEquals(1, pool3.getIdleSize());
        Connection con3 = null;
        try {//
            propertiesSet3.setMockException1(new SQLException("createStatement fail"));
            propertiesSet3.throwsExceptionWhenCallMethod("createStatement");
            LogCollector logCollector = LogCollector.startLogCollector();
            con3 = pool3.getConnection();
            String logs = logCollector.endLogCollector();
            Assertions.assertFalse(logs.contains("connection alive test failed with sql,pool will abandon it"));
            Assertions.fail("testAliveTestFalseBySqlStatement");
        } catch (ConnectionGetTimeoutException e) {
            Assertions.assertEquals("Waited timeout for a released connection", e.getMessage());
        } finally {
            TestUtil.oclose(con3);
            pool3.close();
        }
    }
}
