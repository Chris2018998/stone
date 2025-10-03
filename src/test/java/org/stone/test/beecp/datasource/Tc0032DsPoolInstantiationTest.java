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
import org.stone.beecp.BeeDataSourceCreationException;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.config.DsConfigFactory;
import org.stone.test.beecp.objects.MockCommonXaConnectionFactory;
import org.stone.test.beecp.objects.MockRawConnectionPool;
import org.stone.tools.exception.BeanException;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0032DsPoolInstantiationTest {

    @Test
    public void testDefaultImplementation() throws Exception {
        //1: create during data source construction
        try (BeeDataSource ds = new BeeDataSource(createDefault())) {
            Assertions.assertNull(ds.getPoolImplementClassName());
            Object dsPool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertInstanceOf(FastConnectionPool.class, dsPool);
        }

        //2: lazy create during get connection
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertNull(ds.getPoolImplementClassName());
            ds.setJdbcUrl(DsConfigFactory.JDBC_URL);
            ds.setDriverClassName(DsConfigFactory.JDBC_DRIVER);

            Object dsPool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNull(dsPool);

            try (Connection ignored = ds.getConnection()) {
                dsPool = TestUtil.getFieldValue(ds, "pool");
                Assertions.assertInstanceOf(FastConnectionPool.class, dsPool);
            }

        }

        //3: lazy create during get XAConnection
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertNull(ds.getPoolImplementClassName());
            ds.setXaConnectionFactory(new MockCommonXaConnectionFactory());
            Object dsPool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNull(dsPool);

            XAConnection xaCon = ds.getXAConnection();
            try (Connection ignored = xaCon.getConnection()) {
                dsPool = TestUtil.getFieldValue(ds, "pool");
                Assertions.assertInstanceOf(FastConnectionPool.class, dsPool);
            }
        }
    }

    @Test
    public void testWithConfiguredClassName() throws Exception {
        //1: create during data source construction
        BeeDataSourceConfig config = createDefault();
        config.setPoolImplementClassName(MockRawConnectionPool.class.getName());

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Object dsPool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertFalse(dsPool instanceof FastConnectionPool);
        }

        //2: lazy create during get connection
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertNull(ds.getPoolImplementClassName());
            ds.setPoolImplementClassName(MockRawConnectionPool.class.getName());

            ds.setJdbcUrl(DsConfigFactory.JDBC_URL);
            ds.setDriverClassName(DsConfigFactory.JDBC_DRIVER);

            Object dsPool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNull(dsPool);

            try (Connection ignored = ds.getConnection()) {
                dsPool = TestUtil.getFieldValue(ds, "pool");
                Assertions.assertFalse(dsPool instanceof FastConnectionPool);
            }
        }

        //3: lazy create during get XAConnection
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertNull(ds.getPoolImplementClassName());
            ds.setPoolImplementClassName(MockRawConnectionPool.class.getName());

            ds.setXaConnectionFactory(new MockCommonXaConnectionFactory());
            Object dsPool = TestUtil.getFieldValue(ds, "pool");
            Assertions.assertNull(dsPool);

            XAConnection xaCon = ds.getXAConnection();
            try (Connection ignored = xaCon.getConnection()) {
                dsPool = TestUtil.getFieldValue(ds, "pool");
                Assertions.assertFalse(dsPool instanceof FastConnectionPool);
            }
        }
    }


    @Test
    public void testPoolInstantiatedExceptionInContructor() {
        String poolClassNotFound = "org.stone.test.beecp.objects.MockRawConnectionPool2";
        String poolWithoutDefConstructor = "org.stone.test.beecp.objects.MockPoolImplementation3";
        String isNotPool = "java.lang.String";

        //1: test pool class not found exception
        BeeDataSourceConfig config = createDefault();
        config.setPoolImplementClassName(poolClassNotFound);
        try (BeeDataSource ignored = new BeeDataSource(config)) {
            Assertions.fail("[testPoolInstantiatedException]test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(PoolCreateFailedException.class, e.getCause());
            PoolCreateFailedException poolCreateFailedException = (PoolCreateFailedException) e.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, poolCreateFailedException.getCause());
        }

        //2: test pool class without constructor
        BeeDataSourceConfig config2 = createDefault();
        config2.setPoolImplementClassName(poolWithoutDefConstructor);
        try (BeeDataSource ignored = new BeeDataSource(config2)) {
            Assertions.fail("[testPoolInstantiatedException]test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(PoolCreateFailedException.class, e.getCause());
            PoolCreateFailedException poolCreateFailedException = (PoolCreateFailedException) e.getCause();
            Assertions.assertInstanceOf(BeanException.class, poolCreateFailedException.getCause());
            BeanException beanException = (BeanException) poolCreateFailedException.getCause();
            Assertions.assertTrue(beanException.getMessage().contains("Failed to create instance on class["));
        }

        //3: test pool class without constructor
        BeeDataSourceConfig config3 = createDefault();
        config3.setPoolImplementClassName(isNotPool);
        try (BeeDataSource ignored = new BeeDataSource(config3)) {
            Assertions.fail("[testPoolInstantiatedException]test failed");
        } catch (BeeDataSourceCreationException e) {
            Assertions.assertInstanceOf(PoolCreateFailedException.class, e.getCause());
            PoolCreateFailedException poolCreateFailedException = (PoolCreateFailedException) e.getCause();
            Assertions.assertInstanceOf(BeanException.class, poolCreateFailedException.getCause());
            BeanException cause = (BeanException) poolCreateFailedException.getCause();
            Assertions.assertTrue(cause.getMessage().contains("Can‘t create instance on class[" + isNotPool + "]which must extend from one of type["));
        }
    }


    @Test
    public void testPoolLazyInstantiatedException() throws SQLException {
        String poolClassNotFound = "org.stone.test.beecp.objects.MockRawConnectionPool2";
        String poolWithoutDefConstructor = "org.stone.test.beecp.objects.MockPoolImplementation3";
        String isNotPool = "java.lang.String";

        //1: test pool class not found exception
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setJdbcUrl(DsConfigFactory.JDBC_URL);
            ds.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
            ds.setPoolImplementClassName(poolClassNotFound);
            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testPoolLazyInstantiatedException]test failed");
            } catch (PoolCreateFailedException e) {
                Throwable cause = e.getCause();
                Assertions.assertInstanceOf(ClassNotFoundException.class, cause);
            }
        }

        //2: test pool class without constructor
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setJdbcUrl(DsConfigFactory.JDBC_URL);
            ds.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
            ds.setPoolImplementClassName(poolWithoutDefConstructor);
            try (Connection ignored = ds.getConnection("test", "test")) {
                Assertions.fail("[testPoolLazyInstantiatedException]test failed");
            } catch (PoolCreateFailedException e) {
                Assertions.assertInstanceOf(BeanException.class, e.getCause());
                BeanException beanException = (BeanException) e.getCause();
                Assertions.assertTrue(beanException.getMessage().contains("Failed to create instance on class["));
            }
        }

        //3: test pool class without constructor
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setJdbcUrl(DsConfigFactory.JDBC_URL);
            ds.setDriverClassName(DsConfigFactory.JDBC_DRIVER);
            ds.setPoolImplementClassName(isNotPool);

            try (Connection ignored = ds.getConnection("test", "test")) {
                Assertions.fail("[testPoolLazyInstantiatedException]test failed");
            } catch (PoolCreateFailedException e) {
                Assertions.assertInstanceOf(BeanException.class, e.getCause());
                BeanException cause = (BeanException) e.getCause();
                Assertions.assertTrue(cause.getMessage().contains("Can‘t create instance on class[" + isNotPool + "]which must extend from one of type["));

            }
        }
    }
}
