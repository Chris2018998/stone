/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSourceConfig;

/**
 * @author Chris Liao
 */

public class Tc0004ConfigSetAndGetTest extends TestCase {

    public void testOnSetAndGet() {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();

        //fairMode
        Assert.assertFalse(config.isFairMode());
        config.setFairMode(true);
        Assert.assertTrue(config.isFairMode());

        //asyncCreateInitConnection
        Assert.assertFalse(config.isAsyncCreateInitObject());
        config.setAsyncCreateInitObject(true);
        Assert.assertTrue(config.isAsyncCreateInitObject());

        //borrowSemaphoreSize
        config.setBorrowSemaphoreSize(0);
        Assert.assertNotEquals(config.getBorrowSemaphoreSize(), 0);
        config.setBorrowSemaphoreSize(5);
        Assert.assertEquals(config.getBorrowSemaphoreSize(), 5);

        //maxWait
        config.setMaxWait(0L);
        Assert.assertNotEquals(config.getMaxWait(), 0);
        config.setMaxWait(5000L);
        Assert.assertEquals(config.getMaxWait(), 5000L);

        //idleTimeout
        config.setIdleTimeout(0);
        Assert.assertNotEquals(config.getIdleTimeout(), 0);
        config.setIdleTimeout(3000);
        Assert.assertEquals(config.getIdleTimeout(), 3000);

        //holdTimeout
        config.setHoldTimeout(-1);
        Assert.assertNotEquals(config.getHoldTimeout(), -1);
        config.setHoldTimeout(0);
        Assert.assertEquals(config.getHoldTimeout(), 0);
        config.setHoldTimeout(3000L);
        Assert.assertEquals(config.getHoldTimeout(), 3000L);

        //aliveTestTimeout
        config.setAliveTestTimeout(-1);
        Assert.assertNotEquals(config.getAliveTestTimeout(), -1);
        config.setAliveTestTimeout(0);
        Assert.assertEquals(config.getAliveTestTimeout(), 0);
        config.setAliveTestTimeout(3);
        Assert.assertEquals(config.getAliveTestTimeout(), 3);

        //aliveAssumeTime
        config.setAliveAssumeTime(-1);
        Assert.assertNotEquals(config.getAliveAssumeTime(), -1);
        config.setAliveAssumeTime(0);
        Assert.assertEquals(config.getAliveAssumeTime(), 0);
        config.setAliveAssumeTime(3000L);
        Assert.assertEquals(config.getAliveAssumeTime(), 3000L);

        //timerCheckInterval
        config.setTimerCheckInterval(0);
        Assert.assertNotEquals(config.getTimerCheckInterval(), 0);
        config.setTimerCheckInterval(3000);
        Assert.assertEquals(config.getTimerCheckInterval(), 3000);

        //forceCloseUsingOnClear
        config.setForceCloseUsingOnClear(true);
        Assert.assertTrue(config.isForceCloseUsingOnClear());

        //delayTimeForNextClear
        config.setDelayTimeForNextClear(-1);
        Assert.assertNotEquals(config.getDelayTimeForNextClear(), -1);
        config.setDelayTimeForNextClear(0);
        Assert.assertEquals(config.getDelayTimeForNextClear(), 0L);
        config.setDelayTimeForNextClear(3000L);
        Assert.assertEquals(config.getDelayTimeForNextClear(), 3000L);

        //MaxObjectKeySize
        int maxObjectKeySize = config.getMaxObjectKeySize();
        Assert.assertEquals(maxObjectKeySize, 50);
        config.setMaxObjectKeySize(0);
        Assert.assertEquals(config.getMaxObjectKeySize(), maxObjectKeySize);
        config.setMaxObjectKeySize(1);
        Assert.assertEquals(config.getMaxObjectKeySize(), 1);

        //enableJmx
        config.setEnableJmx(true);
        Assert.assertTrue(config.isEnableJmx());

        //printConfigInfo
        config.setPrintConfigInfo(true);
        Assert.assertTrue(config.isPrintConfigInfo());

        //printRuntimeLog
        config.setPrintRuntimeLog(true);
        Assert.assertTrue(config.isPrintRuntimeLog());

        //printRuntimeLog
        config.setEnableThreadLocal(true);
        Assert.assertTrue(config.isEnableThreadLocal());

        //poolImplementClassName
        config.setPoolImplementClassName(null);
        Assert.assertNotNull(config.getPoolImplementClassName());
        Assert.assertEquals(config.getPoolImplementClassName(), "org.stone.beeop.pool.KeyedObjectPool");
        config.setPoolImplementClassName("org.stone.beeop.pool.KeyedObjectPool");
        Assert.assertEquals(config.getPoolImplementClassName(), "org.stone.beeop.pool.KeyedObjectPool");
    }
}
