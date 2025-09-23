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
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0041EnableLogPrintTest {

    @Test
    public void testSet() throws SQLException {
        try (BeeDataSource ds = new BeeDataSource()) {
            try {
                ds.enableLogPrint(true);
                Assertions.fail();
            } catch (SQLException e) {
                Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
            }

            try {
                if (ds.isEnabledLogPrint()) {
                    System.out.println("isEnabledLogPrint");
                }
                Assertions.fail();
            } catch (SQLException e) {
                Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
            }
        }

        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertFalse(ds.isEnabledLogPrint());
            ds.enableLogPrint(true);
            Assertions.assertTrue(ds.isEnabledLogPrint());
        }
    }
}
