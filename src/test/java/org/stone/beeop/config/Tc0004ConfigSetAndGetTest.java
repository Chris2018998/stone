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

import java.security.InvalidParameterException;

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
        try {
            config.setBorrowSemaphoreSize(-1);
            fail("Setting test failed on configuration item[borrow-semaphore-size]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'borrow-semaphore-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setBorrowSemaphoreSize(0);
            fail("Setting test failed on configuration item[borrow-semaphore-size]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'borrow-semaphore-size' must be greater than zero", e.getMessage());
        }
        config.setBorrowSemaphoreSize(1);
        Assert.assertEquals(1, config.getBorrowSemaphoreSize());

        //maxWait
        try {
            config.setMaxWait(-1L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxWait(0L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        config.setMaxWait(5000L);
        Assert.assertEquals(5000L, config.getMaxWait());

        //idleTimeout
        try {
            config.setIdleTimeout(-1L);
            fail("Setting test failed on configuration item[idle-timeout]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'idle-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setIdleTimeout(0L);
            fail("Setting test failed on configuration item[idle-timeout]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'idle-timeout' must be greater than zero", e.getMessage());
        }
        config.setIdleTimeout(3000L);
        Assert.assertEquals(3000L, config.getIdleTimeout());

        //holdTimeout
        try {
            config.setHoldTimeout(-1L);
            fail("Setting test failed on configuration item[hold-timeout]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'hold-timeout' cannot be less than zero", e.getMessage());
        }
        config.setHoldTimeout(0);
        Assert.assertEquals(0, config.getHoldTimeout());
        config.setHoldTimeout(3000L);
        Assert.assertEquals(3000L, config.getHoldTimeout());

        //aliveTestTimeout
        try {
            config.setAliveTestTimeout(-1);
            fail("Setting test failed on configuration item[alive-test-timeout]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'alive-test-timeout' cannot  be less than zero", e.getMessage());
        }
        config.setAliveTestTimeout(0);
        Assert.assertEquals(0, config.getAliveTestTimeout());
        config.setAliveTestTimeout(3);
        Assert.assertEquals(3, config.getAliveTestTimeout());

        //aliveAssumeTime
        try {
            config.setAliveAssumeTime(-1L);
            fail("Setting test failed on configuration item[alive-assume-time]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'alive-assume-time' cannot be less than zero", e.getMessage());
        }
        config.setAliveAssumeTime(0L);
        Assert.assertEquals(0L, config.getAliveAssumeTime());
        config.setAliveAssumeTime(3000L);
        Assert.assertEquals(3000L, config.getAliveAssumeTime());

        //timerCheckInterval
        try {
            config.setTimerCheckInterval(-1L);
            fail("Setting test failed on configuration item[timer-check-interval]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'timer-check-interval' must be greater than zero", e.getMessage());
        }
        try {
            config.setTimerCheckInterval(0L);
            fail("Setting test failed on configuration item[timer-check-interval]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'timer-check-interval' must be greater than zero", e.getMessage());
        }
        config.setTimerCheckInterval(3000L);
        Assert.assertEquals(3000L, config.getTimerCheckInterval());

        //forceCloseUsingOnClose
        config.setForceRecycleBorrowedOnClose(true);
        Assert.assertTrue(config.isForceRecycleBorrowedOnClose());

        //delayTimeForNextClear
        try {
            config.setParkTimeForRetry(-1L);
            fail("Setting test failed on configuration item[park-time-for-retry]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value for configuration item 'park-time-for-retry' cannot be less than zero", e.getMessage());
        }
        config.setParkTimeForRetry(3000L);
        Assert.assertEquals(3000L, config.getParkTimeForRetry());


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
