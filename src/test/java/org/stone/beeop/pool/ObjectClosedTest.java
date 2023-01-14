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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.object.JavaBookFactory;
import org.stone.beeop.pool.exception.ObjectException;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectClosedTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setInitialSize(5);
        config.setIdleTimeout(3000);
        config.setObjectFactoryClass(JavaBookFactory.class);
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle = null;
        try {
            handle = obs.getObjectHandle();
            handle.close();
            handle.call("toString", new Class[0], new Object[0]);
            TestUtil.assertError("Closed test failed");
        } catch (ObjectException e) {
        } finally {
            if (handle != null)
                handle.close();
        }
    }
}
