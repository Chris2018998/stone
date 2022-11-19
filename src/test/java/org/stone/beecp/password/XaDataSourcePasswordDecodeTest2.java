/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.password;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.RawXaConnectionFactory;
import org.stone.beecp.mock.MockXaDataSource;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

public class XaDataSourcePasswordDecodeTest2 extends TestCase {
    private final String url = "jdbc:mock:test";
    private final String user = "mock";
    private final String password = "root";
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.stone.beecp.mock.MockXaDataSource");
        config.setPasswordDecoderClassName("org.stone.beecp.password.DatabasePasswordDecoder");
        config.addConnectProperty("URL", url);
        config.addConnectProperty("user", user);
        config.addConnectProperty("password", password);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        RawXaConnectionFactory rawXaConnFactory = (RawXaConnectionFactory) TestUtil.getFieldValue(pool, "rawXaConnFactory");
        MockXaDataSource xaDs = (MockXaDataSource) TestUtil.getFieldValue(rawXaConnFactory, "dataSource");

        TestUtil.assertError("user expect value:%s,actual value:%s", user, xaDs.getUser());
        TestUtil.assertError("url expect value:%s,actual value:%s", url, xaDs.getURL());
        TestUtil.assertError("password expect value:%s,actual value:%s", DatabasePasswordDecoder.password(), xaDs.getPassword());
    }
}