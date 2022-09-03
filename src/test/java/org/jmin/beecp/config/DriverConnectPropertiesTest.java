/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beecp.config;

import org.jmin.beecp.*;
import org.jmin.beecp.pool.ConnectionFactoryByDriver;
import org.jmin.beecp.pool.FastConnectionPool;

import java.util.Properties;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class DriverConnectPropertiesTest extends TestCase {
    private BeeDataSource ds;
    private String key1 = "OracleConnection.CONNECTION_PROPERTY_THIN_NET_CONNECT_TIMEOUT";
    private String key2 = "oracle.jdbc.ReadTimeout";

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setDefaultAutoCommit(false);
        config.addConnectProperty(key1, 3000);
        config.addConnectProperty(key2, 6000);
        ds = new BeeDataSource(config);
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        ConnectionFactoryByDriver rawConnFactory = (ConnectionFactoryByDriver) TestUtil.getFieldValue(pool, "rawConnFactory");
        Properties properties = (Properties) TestUtil.getFieldValue(rawConnFactory, "properties");

        if (!JdbcConfig.JDBC_USER.equals(properties.get("user")))
            TestUtil.assertError("user expect value:%s,actual value:%s", JdbcConfig.JDBC_USER, properties.get("user"));
        if (!JdbcConfig.JDBC_PASSWORD.equals(properties.get("password")))
            TestUtil.assertError("password expect value:%s,actual value:%s", JdbcConfig.JDBC_PASSWORD, properties.get("password"));
        if (!new Integer(3000).equals(properties.get(key1)))
            TestUtil.assertError("CONNECT_TIMEOUT expect value:%s,actual value:%s", 3000, properties.get(key1));
        if (!new Integer(6000).equals(properties.get(key2)))
            TestUtil.assertError("oracle.jdbc.ReadTimeout expect value:%s,actual value:%s", 6000, properties.get(key2));
    }
}
