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
import org.stone.beeop.objects.JavaBookFactory;

/**
 * @author Chris Liao
 */

public class Tc0004ConfigSetAndGetTest extends TestCase {

    public void testOnSetAndGet() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
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
        Assert.assertNotEquals(0, config.getBorrowSemaphoreSize());
        config.setBorrowSemaphoreSize(5);
        Assert.assertEquals(5, config.getBorrowSemaphoreSize());

        //maxWait
        config.setMaxWait(0L);
        Assert.assertNotEquals(0, config.getMaxWait());
        config.setMaxWait(5000L);
        Assert.assertEquals(5000L, config.getMaxWait());

        //idleTimeout
        config.setIdleTimeout(0);
        Assert.assertNotEquals(0, config.getIdleTimeout());
        config.setIdleTimeout(3000);
        Assert.assertEquals(3000, config.getIdleTimeout());

        //holdTimeout
        config.setHoldTimeout(-1);
        Assert.assertNotEquals(-1, config.getHoldTimeout());
        config.setHoldTimeout(0);
        Assert.assertEquals(0, config.getHoldTimeout());
        config.setHoldTimeout(3000L);
        Assert.assertEquals(3000L, config.getHoldTimeout());

        //aliveTestTimeout
        config.setAliveTestTimeout(-1);
        Assert.assertNotEquals(-1, config.getAliveTestTimeout());
        config.setAliveTestTimeout(0);
        Assert.assertEquals(0, config.getAliveTestTimeout());
        config.setAliveTestTimeout(3);
        Assert.assertEquals(3, config.getAliveTestTimeout());

        //aliveAssumeTime
        config.setAliveAssumeTime(-1);
        Assert.assertNotEquals(-1, config.getAliveAssumeTime());
        config.setAliveAssumeTime(0);
        Assert.assertEquals(0, config.getAliveAssumeTime());
        config.setAliveAssumeTime(3000L);
        Assert.assertEquals(3000L, config.getAliveAssumeTime());

        //timerCheckInterval
        config.setTimerCheckInterval(0);
        Assert.assertNotEquals(0, config.getTimerCheckInterval());
        config.setTimerCheckInterval(3000);
        Assert.assertEquals(3000, config.getTimerCheckInterval());

        //forceCloseUsingOnClear
        config.setForceRecycleBorrowedOnClose(true);
        Assert.assertTrue(config.isForceRecycleBorrowedOnClose());

        //delayTimeForNextClear
        config.setParkTimeForRetry(-1);
        Assert.assertNotEquals(-1, config.getParkTimeForRetry());
        config.setParkTimeForRetry(0);
        Assert.assertEquals(0L, config.getParkTimeForRetry());
        config.setParkTimeForRetry(3000L);
        Assert.assertEquals(3000L, config.getParkTimeForRetry());

        //MaxObjectKeySize
        int maxObjectKeySize = config.getMaxObjectKeySize();
        Assert.assertEquals(50, maxObjectKeySize);
        config.setMaxObjectKeySize(0);
        Assert.assertEquals(config.getMaxObjectKeySize(), maxObjectKeySize);
        config.setMaxObjectKeySize(1);
        Assert.assertEquals(1, config.getMaxObjectKeySize());

        //object factory
        config.setObjectFactoryClassName(JavaBookFactory.class.getName());
        Assert.assertEquals(JavaBookFactory.class.getName(), config.getObjectFactoryClassName());
        config.setObjectFactoryClass(JavaBookFactory.class);
        Assert.assertEquals(JavaBookFactory.class, config.getObjectFactoryClass());
        JavaBookFactory factory = new JavaBookFactory();
        config.setObjectFactory(factory);
        Assert.assertEquals(factory, config.getObjectFactory());

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
        Assert.assertEquals("org.stone.beeop.pool.KeyedObjectPool", config.getPoolImplementClassName());
        config.setPoolImplementClassName("org.stone.beeop.pool.KeyedObjectPool");
        Assert.assertEquals("org.stone.beeop.pool.KeyedObjectPool", config.getPoolImplementClassName());
    }
}
