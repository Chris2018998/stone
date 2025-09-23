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
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.objects.MockSimpleConnectionFactory;

import java.sql.Connection;
import java.util.Properties;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0040UserPasswordChangeTest {

    @Test
    public void testConfigChange() {
        try (BeeDataSource ds = new BeeDataSource()) {
            String username = "chris";
            String password = "test";
            String url = "jdbc:beecp://localhost/testdb1";
            String url2 = "jdbc:beecp://localhost/testdb2";

            ds.setUsername(username);
            ds.setPassword(password);
            ds.setJdbcUrl(url);

            Assertions.assertEquals(username, ds.getUsername());
            Assertions.assertEquals(password, ds.getPassword());
            Assertions.assertEquals(url, ds.getUrl());
            Assertions.assertEquals(url, ds.getJdbcUrl());
            ds.setJdbcUrl(url2);
            Assertions.assertEquals(url2, ds.getUrl());
            Assertions.assertEquals(url2, ds.getJdbcUrl());
        }
    }


    @Test
    public void testRuntimeChange() throws Exception {
        String username1 = "root";
        String password1 = "root";
        String url1 = "jdbc:beecp://localhost/1testdb";

        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setUsername(username1);
            ds.setPassword(password1);
            ds.setJdbcUrl(url1);
            Connection con1 = null;
            Connection con2 = null;

            try {
                con1 = ds.getConnection();
                Assertions.assertNotNull(con1);
                con2 = ds.getConnection("root", "root");
                Assertions.assertNotNull(con2);

            } finally {
                oclose(con1);
                oclose(con2);
            }

            Object subDs = TestUtil.getFieldValue(ds, "subDs");
            Properties properties = (Properties) TestUtil.getFieldValue(subDs, "properties");

            Assertions.assertEquals(username1, properties.getProperty("user"));
            Assertions.assertEquals(password1, properties.getProperty("password"));
            Assertions.assertEquals(url1, TestUtil.getFieldValue(subDs, "url"));

            String username2 = "chris";
            String password2 = "test";
            String url2 = "jdbc:beecp://localhost/2testdb";
            ds.setUsername(username2);
            ds.setPassword(password2);
            ds.setJdbcUrl(url2);

            Assertions.assertEquals(username2, properties.getProperty("user"));
            Assertions.assertEquals(password2, properties.getProperty("password"));
            Assertions.assertEquals(url2, TestUtil.getFieldValue(subDs, "url"));
        }
    }

    @Test
    public void testExceptionSet() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            MockSimpleConnectionFactory factory = new MockSimpleConnectionFactory();
            ds.setConnectionFactory(factory);

            Connection con1 = null;
            Connection con2 = null;
            try {
                con1 = ds.getConnection();
                Assertions.assertNotNull(con1);
                con2 = ds.getConnection("root", "root");
                Assertions.assertNotNull(con2);

            } finally {
                oclose(con1);
                oclose(con2);
            }

            String username2 = "chris";
            String password2 = "test";
            String url2 = "jdbc:beecp://localhost/2testdb";

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
