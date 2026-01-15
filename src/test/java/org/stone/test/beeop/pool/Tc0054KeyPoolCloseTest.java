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
import org.stone.beeop.exception.BeeObjectSourcePoolNotReadyException;
import org.stone.beeop.pool.ObjectPool;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0054KeyPoolCloseTest {

    @Test
    public void testClose() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        ObjectPool pool = new ObjectPool();
        pool.start(config);

        Assertions.assertNotNull(pool.getObjectHandle());
        pool.close();
        try {
            pool.getObjectHandle();
            fail("Pool close test fail");
        } catch (BeeObjectSourcePoolNotReadyException e) {
            Assertions.assertEquals("Object Internal pool was not ready or closed", e.getMessage());
        }
        //nop
        pool.close();
    }
}
