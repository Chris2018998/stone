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
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.beecp.objects.threads.TimeDelayCloseConnectionThread;

import java.sql.Connection;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0039DataSourceClearTest {

    @Test
    public void testClear() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setForceRecycleBorrowedOnClose(false);//not force close
        config.setParkTimeForRetry(1L);

        //force close borrowed connection
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();//not close it

            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(1, vo.getBorrowedSize());
            ds.clear(true);//force
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getBorrowedSize());
        }

        //delay close borrowed connection
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Connection con = ds.getConnection();//not close it
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(1, vo.getBorrowedSize());
            new TimeDelayCloseConnectionThread(con, System.currentTimeMillis() + 500L).start();

            ds.clear(false);//not force
            vo = ds.getPoolMonitorVo();
            Assertions.assertEquals(0, vo.getBorrowedSize());
        }
    }

    @Test
    public void testReInitialize() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setForceRecycleBorrowedOnClose(false);//not force close
        config.setInitialSize(5);
        config.setMaxActive(10);
        config.setParkTimeForRetry(1L);

        //test a new null configuration
        try (BeeDataSource ds = new BeeDataSource(config)) {
            ds.clear(true, null);
            Assertions.fail("[testReInitialize]test fail");
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertEquals("Pool configuration object can't be null", e.getMessage());
        }

        //test a new configuration
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertEquals(5, ds.getPoolMonitorVo().getIdleSize());
            BeeDataSourceConfig newConfig = createDefault();
            newConfig.setInitialSize(10);
            newConfig.setMaxActive(20);
            ds.clear(true, newConfig);
            Assertions.assertEquals(10, ds.getPoolMonitorVo().getIdleSize());
        }
    }
}
