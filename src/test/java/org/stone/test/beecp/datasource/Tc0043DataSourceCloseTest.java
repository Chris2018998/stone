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

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0043DataSourceCloseTest {

    @Test
    public void testDatasourceClose() throws SQLException {
        BeeDataSource ds1 = new BeeDataSource();
        Assertions.assertTrue(ds1.isClosed());
        ds1.close();//no impact
        Assertions.assertTrue(ds1.isClosed());

        BeeDataSource ds2 = null;
        try {
            BeeDataSourceConfig config = createDefault();
            ds2 = new BeeDataSource(config);
            Assertions.assertFalse(ds2.isClosed());
            try (Connection ignored = ds2.getConnection()) {
                Assertions.assertFalse(ds2.isClosed());
            }
            ds2.close();

            try (Connection ignored = ds2.getConnection()) {
                Assertions.fail("[testDatasourceClose]Test failed");
            } catch (SQLException ee) {
                Assertions.assertEquals("Pool has been closed or is being cleared", ee.getMessage());
            }
        } finally {
            if (ds2 != null && !ds2.isClosed()) {
                ds2.close();
            }
        }
    }
}
