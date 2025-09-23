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
import org.stone.beecp.pool.JdbcCallLogCollectorImpl;
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.BeeJdbcCallLog.Type_GetConnection;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0042EnableLogCollectorTest {

    @Test
    public void testPoolNotCreated() {
        try (BeeDataSource ds = new BeeDataSource()) {
            try {
                ds.enableJdbcCallLogCollector(true);
                Assertions.fail();
            } catch (SQLException e) {
                Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
            }

            try {
                if (ds.isEnabledJdbcCallLogCollector()) {
                    System.out.println("isEnabledMethodLogCollector");
                }
                Assertions.fail();
            } catch (SQLException e) {
                Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
            }

            try {
                ds.getJdbcCallLog(Type_GetConnection);
                Assertions.fail();
            } catch (SQLException e) {
                Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
            }

            try {
                ds.clearJdbcCallLog(0L);
                Assertions.fail();
            } catch (SQLException e) {
                Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
            }

            ds.setJdbcCallLogCollector(new JdbcCallLogCollectorImpl());

        }
    }

    @Test
    public void testNotSetLogCollector() throws SQLException {
        //test when
        BeeDataSourceConfig config = createDefault();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection ignored = ds.getConnection()) {
                Assertions.assertNull(ds.getJdbcCallLog(Type_GetConnection));
                ds.clearJdbcCallLog(0L);
                Assertions.assertNull(ds.getJdbcCallLog(Type_GetConnection));
            }
        }

        //test when exist a log collector
        BeeDataSourceConfig config2 = createDefault();
        config2.setJdbcCallLogCollector(new JdbcCallLogCollectorImpl());
        try (BeeDataSource ds2 = new BeeDataSource(config2)) {
            Assertions.assertTrue(ds2.isEnabledJdbcCallLogCollector());
            ds2.enableJdbcCallLogCollector(false);//disable,all logs will be cleared
            Assertions.assertFalse(ds2.isEnabledJdbcCallLogCollector());

            try (Connection ignored = ds2.getConnection()) {
                Assertions.assertTrue(ds2.getJdbcCallLog(Type_GetConnection).isEmpty());
                ds2.clearJdbcCallLog(0L);
                Assertions.assertTrue(ds2.getJdbcCallLog(Type_GetConnection).isEmpty());
            }
        }
    }
}
