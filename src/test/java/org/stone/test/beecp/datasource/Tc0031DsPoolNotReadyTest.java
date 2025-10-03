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
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import java.sql.SQLException;

/**
 * @author Chris Liao
 */
public class Tc0031DsPoolNotReadyTest {

    @Test
    public void testPoolNotCreatedException() throws SQLException {
        //1: Pool Creation in BeeDataSource constructor: new BeeDataSource(BeeDataSourceConfig config)
        //2: Pool Lazy Creation(ds.getConnection(),ds.getXAConnection(),ds.getConnection(String,String),ds.getXAConnection(String,String))

        try (BeeDataSource ds = new BeeDataSource()) {
            try {
                ds.clear(false);
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.clear(false, new BeeDataSourceConfig());
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.enableLogPrint(false);
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.isEnabledLogPrint();
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.enableJdbcCallLogCollector(false);
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.clearJdbcCallLog();
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.getJdbcCallLog(BeeJdbcCallLog.Type_Get_Connection);
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }


            try {
                ds.isEnabledJdbcCallLogCollector();
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.getPoolMonitorVo();
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }

            try {
                ds.interruptConnectionCreating(true);
                Assertions.fail("[testDsPoolNotReady]test failed");
            } catch (PoolNotCreatedException e) {
                Assertions.assertEquals("Data source pool not be instantiated", e.getMessage());
            }
        }
    }
}
