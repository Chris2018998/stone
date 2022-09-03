/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beecp.password;

import org.jmin.beecp.*;
import org.jmin.beecp.mock.MockXaDataSource;
import org.jmin.beecp.pool.FastConnectionPool;

public class XaDataSourcePasswordDecodeTest2 extends TestCase {
    private final String url = "jdbc:mock:test";
    private final String user = "mock";
    private final String password = "root";
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setConnectionFactoryClassName("org.jmin.beecp.mock.MockXaDataSource");
        config.setPasswordDecoderClassName("org.jmin.beecp.password.DatabasePasswordDecoder");
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

        if (!user.equals(xaDs.getUser()))
            TestUtil.assertError("user expect value:%s,actual value:%s", user, xaDs.getUser());
        if (!url.equals(xaDs.getURL()))
            TestUtil.assertError("url expect value:%s,actual value:%s", url, xaDs.getURL());
        if (!DatabasePasswordDecoder.password().equals(xaDs.getPassword()))
            TestUtil.assertError("password expect value:%s,actual value:%s", DatabasePasswordDecoder.password(), xaDs.getPassword());
    }
}