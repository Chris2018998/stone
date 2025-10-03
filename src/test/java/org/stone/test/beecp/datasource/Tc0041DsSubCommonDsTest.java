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
import org.stone.test.beecp.objects.MockCommonConnectionFactory;

import java.io.PrintWriter;
import java.sql.SQLFeatureNotSupportedException;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0041DsSubCommonDsTest {

    @Test
    public void testSetOnCommonDs() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setForceRecycleBorrowedOnClose(false);//not force close
        config.setParkTimeForRetry(1L);
        config.setConnectionFactoryClassName("org.stone.test.beecp.driver.MockDataSource");

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNull(ds.getLogWriter());
            Assertions.assertNull(ds.getParentLogger());
            Assertions.assertEquals(0, ds.getLoginTimeout());

            PrintWriter logWriter = new PrintWriter(System.out);
            ds.setLogWriter(logWriter);
            ds.setLoginTimeout(5);
            Assertions.assertEquals(logWriter, ds.getLogWriter());
            Assertions.assertEquals(5, ds.getLoginTimeout());
        }
    }

    @Test
    public void testSetOnNullCommonDs() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertNull(ds.getLogWriter());
            Assertions.assertNull(ds.getParentLogger());
            Assertions.assertEquals(0, ds.getLoginTimeout());

            ds.setLogWriter(new PrintWriter(System.out));
            ds.setLoginTimeout(5);
            Assertions.assertNull(ds.getLogWriter());
            Assertions.assertEquals(0, ds.getLoginTimeout());
        }
    }

    @Test
    public void testExceptionFromCommonDs() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setForceRecycleBorrowedOnClose(false);//not force close
        config.setParkTimeForRetry(1L);
        config.setConnectionFactory(new MockCommonConnectionFactory());
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try {
                ds.getLogWriter();
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }

            try {
                ds.getParentLogger();
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }

            try {
                ds.getLoginTimeout();
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }

            try {
                ds.setLoginTimeout(7);
                Assertions.fail("[testSetOnNullCommonDs]test failed");
            } catch (SQLFeatureNotSupportedException e) {
                //do nothing
            }
        }
    }
}
