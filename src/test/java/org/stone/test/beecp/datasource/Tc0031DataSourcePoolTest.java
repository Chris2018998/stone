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
import org.stone.beecp.*;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.FastConnectionPool4L;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beecp.pool.exception.PoolInitializeFailedException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;
import org.stone.test.base.LogCollector;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockJdbcCallLogCollector;

import javax.sql.XAConnection;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */
public class Tc0031DataSourcePoolTest {

    @Test
    public void testOnInitializedPool() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        //config.setCreateTimeout(5);
        config.setPrintConfigInfo(false);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            LogCollector logCollector = LogCollector.startLogCollector();
            //getConnection test
            Connection con1 = ds.getConnection();
            Assertions.assertNotNull(con1);
            con1.close();
            con1 = ds.getConnection(JDBC_USER, JDBC_PASSWORD);
            Assertions.assertNotNull(con1);
            con1.close();

            //getXAConnection test
            XAConnection xCon1 = ds.getXAConnection();
            Assertions.assertNotNull(xCon1);
            xCon1.close();
            xCon1 = ds.getXAConnection(JDBC_USER, JDBC_PASSWORD);
            Assertions.assertNotNull(xCon1);
            xCon1.close();

            String logs = logCollector.endLogCollector();
            Assertions.assertTrue(logs.contains("getConnection (user,password) ignores authentication"));
            Assertions.assertTrue(logs.contains("getXAConnection (user,password) ignores authentication"));

            //test on methods of commonDataSource
            Assertions.assertNull(ds.getParentLogger());
            Assertions.assertNull(ds.getLogWriter());
            ds.setLogWriter(new PrintWriter(System.out));
            Assertions.assertNotNull(ds.getLogWriter());
            ds.setLogWriter(null);//reset back to null
            Assertions.assertNull(ds.getLogWriter());
            DriverManager.setLoginTimeout(0);
            Assertions.assertEquals(0, ds.getLoginTimeout());
            Assertions.assertEquals(0, DriverManager.getLoginTimeout());
            ds.setLoginTimeout(10);//ten seconds
            Assertions.assertEquals(10, ds.getLoginTimeout());
            Assertions.assertEquals(10, DriverManager.getLoginTimeout());

            //test on printRuntimeLog ind
            BeeConnectionPool pool = (BeeConnectionPool) TestUtil.getFieldValue(ds, "pool");
            Boolean printRuntimeLogInd = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
            Assertions.assertFalse(printRuntimeLogInd);
            ds.setPrintRuntimeLog(true);
            printRuntimeLogInd = (Boolean) TestUtil.getFieldValue(pool, "printRuntimeLog");
            Assertions.assertTrue(printRuntimeLogInd);

            //test poolMonitorVo
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertNotNull(vo);
            Assertions.assertEquals(1, vo.getIdleSize());

            //test on pool built-in lock
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getCreatingCount());
            Assertions.assertEquals(0, vo.getCreatingTimeoutCount());
            Thread[] interruptedThreads = ds.interruptConnectionCreating(false);
            Assertions.assertNotNull(interruptedThreads);
            Assertions.assertEquals(0, interruptedThreads.length);

            //maxWait
            try {
                ds.setMaxWait(-1L);
                fail("Max wait time test failed");
            } catch (InvalidParameterException e) {
                Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
            }
            try {
                ds.setMaxWait(0L);
                fail("Max wait time test failed");
            } catch (InvalidParameterException e) {
                Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
            }
            long newMaxWaitMillis2 = TimeUnit.SECONDS.toMillis(20L);
            ds.setMaxWait(newMaxWaitMillis2);
            Assertions.assertEquals(newMaxWaitMillis2, ds.getMaxWait());//changed
        }
    }

    @Test
    public void testDsClose() {
        BeeDataSource ds1 = new BeeDataSource();
        Assertions.assertTrue(ds1.isClosed());

        BeeDataSource ds2 = new BeeDataSource(createDefault());
        Assertions.assertFalse(ds2.isClosed());
        ds2.close();
        Assertions.assertTrue(ds2.isClosed());
    }

    @Test
    public void testOnUninitializedPool() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            BeeConnectionPool pool = (BeeConnectionPool) TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNull(pool);

            //test on methods of commonDataSource
            Assertions.assertNull(ds.getParentLogger());
            Assertions.assertNull(ds.getLogWriter());
            ds.setLogWriter(new PrintWriter(System.out));
            Assertions.assertNull(ds.getLogWriter());
            Assertions.assertEquals(0, ds.getLoginTimeout());
            Assertions.assertEquals(0, DriverManager.getLoginTimeout());
            ds.setLoginTimeout(10);//ten seconds
            Assertions.assertEquals(0, ds.getLoginTimeout());
            Assertions.assertEquals(0, DriverManager.getLoginTimeout());

            ds.setPrintRuntimeLog(true);
            try {
                ds.getPoolMonitorVo();
                fail("testOnUninitializedPool");
            } catch (PoolNotCreatedException e) {
                Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
            }

            try {
                ds.interruptConnectionCreating(false);
                fail("testOnUninitializedPool");
            } catch (PoolNotCreatedException e) {
                Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
            }

            try {
                ds.clear(true);
                fail("testOnUninitializedPool");
            } catch (PoolNotCreatedException e) {
                Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
            }

            try {
                ds.clear(true, new BeeDataSourceConfig());
                fail("testOnUninitializedPool");
            } catch (PoolNotCreatedException e) {
                Assertions.assertTrue(e.getMessage().contains("Pool not be created"));
            }
        }


        BeeDataSourceConfig config = createDefault();
        MockCommonConnectionFactory factory = new MockCommonConnectionFactory();
        factory.setReturnNullOnCreate(true);
        config.setConnectionFactory(factory);
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            //do nothing
        }
    }

    @Test
    public void testPoolClassNotFound() {
        try (BeeDataSource ds = new BeeDataSource(JDBC_DRIVER, JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {//lazy creation
            ds.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            try (Connection con = ds.getConnection()) {
                fail("testPoolClassNotFound");
            }
        } catch (SQLException e) {
            Throwable poolCause = e.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCause);
        }

        BeeDataSourceConfig config = createDefault();
        config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
        try (BeeDataSource ignored = new BeeDataSource(config)) {//creation in constructor
            fail("testPoolClassNotFound");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(PoolCreateFailedException.class, cause);
            PoolCreateFailedException poolException = (PoolCreateFailedException) cause;
            Throwable poolCause = poolException.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCause);
        }
    }

    @Test
    public void testPoolInitializeFailedException() {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(5);
        config.setInitialSize(10);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            fail("testPoolInitializeFailedException");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assertions.assertInstanceOf(PoolInitializeFailedException.class, cause);
            PoolInitializeFailedException poolInitializeException = (PoolInitializeFailedException) cause;
            Assertions.assertInstanceOf(BeeDataSourceConfigException.class, poolInitializeException.getCause());
            Throwable bottomException = poolInitializeException.getCause();

            System.out.println(bottomException.getMessage());
            //Assertions.assertTrue(bottomException.getMessage().equals("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'"));
        }
    }

    @Test
    public void testTracePoolCreation() throws Exception {
        BeeDataSourceConfig config1 = createEmpty();
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        config1.setJdbcCallLogCollector(new MockJdbcCallLogCollector());
        try (BeeDataSource ds1 = new BeeDataSource(config1)) {
            Assertions.assertInstanceOf(FastConnectionPool4L.class, TestUtil.getFieldValue(ds1, "pool"));
        }

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setJdbcCallLogCollectorClass(MockJdbcCallLogCollector.class);
        try (BeeDataSource ds2 = new BeeDataSource(config2)) {
            Assertions.assertInstanceOf(FastConnectionPool4L.class, TestUtil.getFieldValue(ds2, "pool"));
        }

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setJdbcCallLogCollectorClassName(MockJdbcCallLogCollector.class.getName());
        try (BeeDataSource ds3 = new BeeDataSource(config3)) {
            Assertions.assertInstanceOf(FastConnectionPool4L.class, TestUtil.getFieldValue(ds3, "pool"));
        }

        BeeDataSourceConfig config4 = createEmpty();
        config4.setConnectionFactory(connectionFactory);
        try (BeeDataSource ds4 = new BeeDataSource(config4)) {
            Assertions.assertInstanceOf(FastConnectionPool.class, TestUtil.getFieldValue(ds4, "pool"));
        }
    }
}
