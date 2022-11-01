/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.beeop.*;
import org.stone.beeop.object.JavaBook;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectClassTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectClass(JavaBook.class);
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle = null;
        try {
            handle = obs.getObject();
            if (handle == null)
                TestUtil.assertError("Failed to get object");
        } finally {
            if (handle != null)
                handle.close();
        }
    }
}
