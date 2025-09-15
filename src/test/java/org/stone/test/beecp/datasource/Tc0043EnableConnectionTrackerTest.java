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
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockConnectionTracker;

import java.sql.SQLException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0043EnableConnectionTrackerTest {

    @Test
    public void testSet() throws SQLException {
        BeeDataSource ds = new BeeDataSource();
        Assertions.assertNull(ds.getConnectionTracker());
        MockConnectionTracker tracker = new MockConnectionTracker();
        ds.setConnectionTracker(tracker);
        Assertions.assertEquals(tracker, ds.getConnectionTracker());

        try {
            ds.enableConnectionTracker(true);
            Assertions.fail();
        } catch (SQLException e) {
            Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
        }

        try {
            if (ds.isEnabledConnectionTracker()) {
                System.out.println("isEnabledConnectionTracker");
            }
            Assertions.fail();
        } catch (SQLException e) {
            Assertions.assertInstanceOf(PoolNotCreatedException.class, e);
        }

        BeeDataSourceConfig config = createEmpty();
        config.setConnectionFactory(new MockCommonConnectionFactory());
        ds = new BeeDataSource(config);
        Assertions.assertFalse(ds.isEnabledConnectionTracker());
        try {
            ds.enableConnectionTracker(true);
        } catch (RuntimeException e) {
            Assertions.assertInstanceOf(BeeDataSourceConfigException.class, e);
        }
        ds.enableConnectionTracker(false);
        Assertions.assertFalse(ds.isEnabledConnectionTracker());
        ds.setConnectionTracker(tracker);
        Assertions.assertTrue(ds.isEnabledConnectionTracker());

        config.setConnectionTracker(new MockConnectionTracker());
        ds = new BeeDataSource(config);
        Assertions.assertTrue(ds.isEnabledConnectionTracker());
        ds.enableConnectionTracker(false);
        Assertions.assertFalse(ds.isEnabledConnectionTracker());
        ds.enableConnectionTracker(true);
        Assertions.assertTrue(ds.isEnabledConnectionTracker());
    }

}
