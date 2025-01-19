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
import org.stone.beeop.pool.exception.ObjectGetForbiddenException;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0054KeyPoolCloseTest extends TestCase {

    public void testClose() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        Assert.assertNotNull(pool.getObjectHandle());
        pool.close();
        try {
            pool.getObjectHandle();
            fail("Pool close test fail");
        } catch (ObjectGetForbiddenException e) {
            Assert.assertEquals("Object pool was not ready or closed", e.getMessage());
        }
        //nop
        pool.close();
    }
}
