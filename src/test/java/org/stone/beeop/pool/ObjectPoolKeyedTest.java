/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPool;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.object.JavaBookFactory;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectPoolKeyedTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactoryClass(JavaBookFactory.class);
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle1 = null;
        BeeObjectHandle handle2 = null;

        String testKey = "testKey";
        handle1 = obs.getObjectHandle();
        handle2 = obs.getObjectHandle(testKey);
        if (!testKey.equals(handle2.getObjectKey())) TestUtil.assertError("Object key test failed");

        BeeObjectPool pool = (BeeObjectPool) TestUtil.getFieldValue(obs, "pool");
        Object[] keys = pool.keys();
        System.out.println("size: " + keys.length);
        if (keys.length != 2) TestUtil.assertError("Object key test failed");
        if (keys[0] != null) TestUtil.assertError("Object key test failed");
        if (!keys[1].equals(testKey)) TestUtil.assertError("Object key test failed");

        if (handle1 != null) handle1.close();
        if (handle2 != null) handle2.close();

        pool.deleteKey(testKey);
        keys = pool.keys();
        if (keys.length != 1) TestUtil.assertError("Object key test failed");
    }
}
