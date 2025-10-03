/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;

import java.sql.Connection;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0033ConnectionGetTest {

    @Test
    public void testGetConnectionByDriver() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setUsername("root");
            ds.setPassword("root");
            ds.setUrl("jdbc:beecp://localhost/testdb");
            ds.setDriverClassName("org.stone.test.beecp.driver.MockDriver");

            try (Connection con1 = ds.getConnection(); Connection con2 = ds.getConnection("root", "root")) {
                Assertions.assertNotNull(con1);
                Assertions.assertNotNull(con2);
            }
        }
    }

    @Test
    public void testGetConnectionByDriverDs() throws Exception {
        Connection con1 = null;
        Connection con2 = null;

        try (BeeDataSource ds2 = new BeeDataSource()) {
            ds2.setUsername("root");
            ds2.setPassword("root");
            ds2.setConnectionFactoryClassName("org.stone.test.beecp.driver.MockDataSource");
            try {
                con1 = ds2.getConnection();
                Assertions.assertNotNull(con1);
                con2 = ds2.getConnection("root", "root");
                Assertions.assertNotNull(con2);
            } finally {
                oclose(con1);
                oclose(con2);
            }
        }
    }

    @Test
    public void testGetConnectionByFactory() throws Exception {
        String dataSourceClassName = "org.stone.test.beecp.objects.MockDriverConnectionFactory";

        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setConnectionFactoryClassName(dataSourceClassName);
            Connection con1 = null;
            Connection con2 = null;

            try {
                con1 = ds.getConnection(null, null);
                Assertions.assertNotNull(con1);
                con2 = ds.getConnection();
                Assertions.assertNotNull(con2);
            } finally {
                oclose(con1);
                oclose(con2);
            }
        }
    }

}