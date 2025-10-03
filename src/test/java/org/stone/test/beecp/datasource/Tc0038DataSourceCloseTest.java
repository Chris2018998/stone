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
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.beecp.config.DsConfigFactory;
import org.stone.test.beecp.objects.threads.TimeDelayCloseConnectionThread;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0038DataSourceCloseTest {

    @Test
    public void testClose() throws SQLException {
        BeeDataSource ds = null;
        try {
            BeeDataSourceConfig config = createDefault();
            config.setMaxActive(10);
            config.setInitialSize(5);
            ds = new BeeDataSource(config);
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(5, vo.getIdleSize());
            Assertions.assertFalse(ds.isClosed());
            ds.close();
            Assertions.assertTrue(ds.isClosed());
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getIdleSize());

            ds.close();//test close twice
        } catch (RuntimeException e) {
            Assertions.fail("[testClose]test failed");
        } finally {
            if (ds != null && !ds.isClosed()) ds.close();
        }
    }

    @Test
    public void testCloseLazyInitialization() throws SQLException {
        BeeDataSource ds = null;
        try {
            ds = new BeeDataSource();
            Assertions.assertTrue(ds.isClosed());
            ds.setMaxActive(10);
            ds.setInitialSize(5);
            ds.setJdbcUrl(DsConfigFactory.JDBC_URL);
            ds.setDriverClassName(DsConfigFactory.JDBC_DRIVER);

            try (Connection ignored = ds.getConnection()) {
                BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
                Assertions.assertEquals(1, vo.getBorrowedSize());
                Assertions.assertEquals(4, vo.getIdleSize());
            }
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getBorrowedSize());
            Assertions.assertEquals(5, vo.getIdleSize());

            ds.close();
            Assertions.assertTrue(ds.isClosed());
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getIdleSize());

            ds.close();//test close twice
        } catch (RuntimeException e) {
            Assertions.fail("[testClose]test failed");
        } finally {
            if (ds != null && !ds.isClosed()) ds.close();
        }
    }

    @Test
    public void testForceCloseBorrowedConnection() throws SQLException {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(10);
        config.setInitialSize(5);
        config.setForceRecycleBorrowedOnClose(true);//force close
        BeeDataSource ds = null;

        try {
            ds = new BeeDataSource(config);
            ds.getConnection();//not close it
        } finally {
            if (ds != null) {
                ds.close();
                Assertions.assertTrue(ds.isClosed());
                Assertions.assertEquals(0, ds.getPoolMonitorVo().getBorrowedSize());
            }
        }
    }

    @Test
    public void testDelayCloseBorrowedConnection() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(10);
        config.setInitialSize(5);
        config.setForceRecycleBorrowedOnClose(false);//not force close
        config.setParkTimeForRetry(1L);//1 millisecond
        BeeDataSource ds = null;

        try {
            ds = new BeeDataSource(config);
            Connection con = ds.getConnection();
            new TimeDelayCloseConnectionThread(con, System.currentTimeMillis() + 500L).start();
        } finally {
            if (ds != null) {
                ds.close();
                Assertions.assertTrue(ds.isClosed());
                Assertions.assertEquals(0, ds.getPoolMonitorVo().getBorrowedSize());
            }
        }
    }
}
