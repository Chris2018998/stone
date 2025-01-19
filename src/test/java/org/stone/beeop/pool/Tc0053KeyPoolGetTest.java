/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.pool.exception.ObjectKeyException;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0053KeyPoolGetTest extends TestCase {

    public void testPoolNotReady() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setMaxKeySize(2);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        String testKey = "pool2";
        Assert.assertFalse(pool.exists(testKey));
        Assert.assertNotNull(pool.getObjectHandle(testKey));
        Assert.assertNotNull(pool.getObjectHandle(testKey));

        try {
            String testKey2 = "pool3";
            Assert.assertNotNull(pool.getObjectHandle(testKey2));
            fail("Object get test failed");
        } catch (ObjectKeyException e) {
            Assert.assertEquals("Object category capacity of pool has reach max size:2", e.getMessage());
        }
    }
}
