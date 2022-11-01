/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

import org.stone.beecp.*;

public class PoolRestTest extends TestCase {
    private final int initSize = 5;
    private BeeDataSource ds;

    public void setUp() throws Throwable {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcUrl(JdbcConfig.JDBC_URL);
        config.setDriverClassName(JdbcConfig.JDBC_DRIVER);
        config.setUsername(JdbcConfig.JDBC_USER);
        config.setPassword(JdbcConfig.JDBC_PASSWORD);
        config.setInitialSize(initSize);
        ds = new BeeDataSource(config);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        FastConnectionPool pool = (FastConnectionPool) TestUtil.getFieldValue(ds, "pool");
        if (pool.getTotalSize() != initSize)
            TestUtil.assertError("Total connections expected:%s,current is:%s", initSize, pool.getTotalSize());
        if (pool.getIdleSize() != initSize)
            TestUtil.assertError("connections expected:%s,current is:%s", initSize, pool.getIdleSize());

        pool.clear();

        if (pool.getTotalSize() != 0)
            TestUtil.assertError("Total connections not as expected 0,but current is:%s", pool.getTotalSize(), "");
        if (pool.getIdleSize() != 0)
            TestUtil.assertError("Idle connections not as expected 0,but current is:%s", pool.getIdleSize(), "");
    }
}
