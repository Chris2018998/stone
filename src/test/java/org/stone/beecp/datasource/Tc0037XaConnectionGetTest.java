/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.datasource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import java.io.PrintWriter;

import static org.stone.beecp.pool.ConnectionPoolStatics.oclose;

/**
 * @author Chris Liao
 */
public class Tc0037XaConnectionGetTest extends TestCase {

    public void testGetConnectionByDriverDs() throws Exception {
        String dataSourceClassName = "org.stone.beecp.driver.MockXaDataSource";
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName(dataSourceClassName);
        BeeDataSource ds = new BeeDataSource(config);

        XAConnection con1 = null;
        XAConnection con2 = null;
        try {
            con1 = ds.getXAConnection();
            Assert.assertNotNull(con1);
            con2 = ds.getXAConnection("root", "root");
            Assert.assertNotNull(con2);

            Assert.assertNull(ds.getLogWriter());
            Assert.assertNull(ds.getParentLogger());
            ds.setLogWriter(new PrintWriter(System.out));
            Assert.assertNotNull(ds.getLogWriter());
            Assert.assertEquals(0, ds.getLoginTimeout());
            ds.setLoginTimeout(10);
            Assert.assertEquals(10, ds.getLoginTimeout());
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }

    public void testGetConnectionByFactory() throws Exception {
        String dataSourceClassName = "org.stone.beecp.objects.MockDriverXaConnectionFactory";
        BeeDataSource ds = new BeeDataSource();
        ds.setConnectionFactoryClassName(dataSourceClassName);

        XAConnection con1 = null;
        XAConnection con2 = null;
        try {
            con1 = ds.getXAConnection("root", "root");
            Assert.assertNotNull(con1);

            con2 = ds.getXAConnection();
            Assert.assertNotNull(con2);
        } finally {
            oclose(con1);
            oclose(con2);
        }
    }
}
