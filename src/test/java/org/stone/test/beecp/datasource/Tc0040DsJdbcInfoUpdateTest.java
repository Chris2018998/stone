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
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.MockSimpleConnectionFactory;

import java.util.Properties;

import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0040DsJdbcInfoUpdateTest {
    private final String username1 = "chris1";
    private final String password1 = "test1";
    private final String url1 = "jdbc:beecp://localhost/testdb1";

    private final String username2 = "chris2";
    private final String password2 = "test2";
    private final String url2 = "jdbc:beecp://localhost/testdb2";

    @Test
    public void testUpdateToConfig() {
        try (BeeDataSource ds = new BeeDataSource()) {
            Assertions.assertTrue(ds.isClosed());//ds pool not ready
            Assertions.assertNull(ds.getUsername());
            Assertions.assertNull(ds.getPassword());
            Assertions.assertNull(ds.getJdbcUrl());

            ds.setUsername(username1);
            ds.setPassword(password1);
            ds.setJdbcUrl(url1);

            Assertions.assertEquals(username1, ds.getUsername());
            Assertions.assertEquals(password1, ds.getPassword());
            Assertions.assertEquals(url1, ds.getJdbcUrl());
        }
    }

    @Test
    public void testUpdateToPool() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setUsername(username1);
        config.setPassword(password1);
        config.setJdbcUrl(url1);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertFalse(ds.isClosed());//ds pool ready

            //before update
            Object subDs = TestUtil.getFieldValue(ds, "subDs");
            Properties properties = (Properties) TestUtil.getFieldValue(subDs, "properties");
            Assertions.assertEquals(username1, properties.getProperty("user"));
            Assertions.assertEquals(password1, properties.getProperty("password"));
            Assertions.assertEquals(url1, TestUtil.getFieldValue(subDs, "url"));

            //after update
            ds.setUsername(username2);
            ds.setPassword(password2);
            ds.setJdbcUrl(url2);
            properties = (Properties) TestUtil.getFieldValue(subDs, "properties");
            Assertions.assertEquals(username2, properties.getProperty("user"));
            Assertions.assertEquals(password2, properties.getProperty("password"));
            Assertions.assertEquals(url2, TestUtil.getFieldValue(subDs, "url"));
        }
    }

    @Test
    public void testFailUpdateToPool() {
        BeeDataSourceConfig config = createDefault();
        config.setConnectionFactory(new MockSimpleConnectionFactory());

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertFalse(ds.isClosed());//ds pool ready

            try {
                ds.setUsername(username2);
                Assertions.fail();
            } catch (RuntimeException e) {
                Assertions.assertInstanceOf(NoSuchMethodException.class, e.getCause());
            }

            try {
                ds.setPassword(password2);
                Assertions.fail();
            } catch (RuntimeException e) {
                Assertions.assertInstanceOf(NoSuchMethodException.class, e.getCause());
            }

            try {
                ds.setJdbcUrl(url2);
                Assertions.fail();
            } catch (RuntimeException e) {
                Assertions.assertInstanceOf(NoSuchMethodException.class, e.getCause());
            }
        }
    }
}
