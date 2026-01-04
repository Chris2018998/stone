/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.ObjectKeyException;
import org.stone.beeop.pool.KeyedObjectPool;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0053KeyPoolGetTest {

    @Test
    public void testPoolNotReady() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setMaxKeySize(2);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.start(config);

        String testKey = "pool2";
        Assertions.assertFalse(pool.exists(testKey));
        Assertions.assertNotNull(pool.getObjectHandle(testKey));
        Assertions.assertNotNull(pool.getObjectHandle(testKey));

        try {
            String testKey2 = "pool3";
            Assertions.assertNotNull(pool.getObjectHandle(testKey2));
            fail("Object get test failed");
        } catch (ObjectKeyException e) {
            Assertions.assertEquals("Object category capacity of pool has reach max size:2", e.getMessage());
        }
    }
}
