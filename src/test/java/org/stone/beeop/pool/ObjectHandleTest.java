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

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectHandleTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactoryClassName(JavaBookFactory.class.getName());
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle = null;
        try {
            handle = obs.getObjectHandle();
            if (handle == null) TestUtil.assertError("Failed to get object");
            if (handle.getObjectKey() != null) TestUtil.assertError("object key is not null");
        } finally {
            if (handle != null)
                handle.close();
        }
    }
}
