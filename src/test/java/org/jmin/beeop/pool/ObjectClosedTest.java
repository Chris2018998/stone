/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop.pool;

import org.jmin.beeop.*;
import org.jmin.beeop.object.JavaBookFactory;
import org.jmin.beeop.pool.exception.ObjectException;

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
            handle = obs.getObject();
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
