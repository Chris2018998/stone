/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop.config;

import org.jmin.beeop.BeeObjectSource;
import org.jmin.beeop.BeeObjectSourceConfig;
import org.jmin.beeop.TestCase;
import org.jmin.beeop.TestUtil;
import org.jmin.beeop.object.JavaBookFactory;
import org.jmin.beeop.pool.FastObjectPool;

public class PassedConfigUnchangeableTest extends TestCase {
    private final int initSize = 5;
    private final int maxSize = 20;
    BeeObjectSourceConfig testConfig;
    private BeeObjectSource ds;

    public void setUp() throws Throwable {
        testConfig = new BeeObjectSourceConfig();
        testConfig.setObjectFactoryClassName(JavaBookFactory.class.getName());
        testConfig.setInitialSize(initSize);
        testConfig.setMaxActive(maxSize);
        testConfig.setIdleTimeout(3000);
        ds = new BeeObjectSource(testConfig);
    }

    public void tearDown() throws Throwable {
        ds.close();
    }

    public void test() throws Exception {
        testConfig.setInitialSize(10);
        testConfig.setMaxActive(50);

        FastObjectPool pool = (FastObjectPool) TestUtil.getFieldValue(ds, "pool");
        BeeObjectSourceConfig tempConfig = (BeeObjectSourceConfig) TestUtil.getFieldValue(pool, "poolConfig");
        if (tempConfig.getInitialSize() != initSize) TestUtil.assertError("initSize has changed,expected:" + initSize);
        if (tempConfig.getMaxActive() != maxSize) TestUtil.assertError("maxActive has changed,expected" + maxSize);
    }
}
