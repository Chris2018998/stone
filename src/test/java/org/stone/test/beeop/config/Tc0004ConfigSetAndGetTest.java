/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.JavaBookFactory;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */

public class Tc0004ConfigSetAndGetTest {

    @Test
    public void testOnSetAndGet() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        //fairMode
        Assertions.assertFalse(config.isFairMode());
        config.setFairMode(true);
        Assertions.assertTrue(config.isFairMode());

        //asyncCreateInitConnection
        Assertions.assertFalse(config.isAsyncCreateInitObject());
        config.setAsyncCreateInitObject(true);
        Assertions.assertTrue(config.isAsyncCreateInitObject());

        //borrowSemaphoreSize
        try {
            config.setSemaphoreSize(-1);
            fail("Setting test failed on configuration item[borrow-semaphore-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'borrow-semaphore-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setSemaphoreSize(0);
            fail("Setting test failed on configuration item[borrow-semaphore-size]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'borrow-semaphore-size' must be greater than zero", e.getMessage());
        }
        config.setSemaphoreSize(1);
        Assertions.assertEquals(1, config.getSemaphoreSize());

        //maxWait
        try {
            config.setMaxWait(-1L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxWait(0L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        config.setMaxWait(5000L);
        Assertions.assertEquals(5000L, config.getMaxWait());

        //idleTimeout
        try {
            config.setIdleTimeout(-1L);
            fail("Setting test failed on configuration item[idle-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'idle-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setIdleTimeout(0L);
            fail("Setting test failed on configuration item[idle-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'idle-timeout' must be greater than zero", e.getMessage());
        }
        config.setIdleTimeout(3000L);
        Assertions.assertEquals(3000L, config.getIdleTimeout());

        //holdTimeout
        try {
            config.setHoldTimeout(-1L);
            fail("Setting test failed on configuration item[hold-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'hold-timeout' cannot be less than zero", e.getMessage());
        }
        config.setHoldTimeout(0);
        Assertions.assertEquals(0, config.getHoldTimeout());
        config.setHoldTimeout(3000L);
        Assertions.assertEquals(3000L, config.getHoldTimeout());

        //aliveTestTimeout
        try {
            config.setAliveTestTimeout(-1);
            fail("Setting test failed on configuration item[alive-test-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'alive-test-timeout' cannot  be less than zero", e.getMessage());
        }
        config.setAliveTestTimeout(0);
        Assertions.assertEquals(0, config.getAliveTestTimeout());
        config.setAliveTestTimeout(3);
        Assertions.assertEquals(3, config.getAliveTestTimeout());

        //aliveAssumeTime
        try {
            config.setAliveAssumeTime(-1L);
            fail("Setting test failed on configuration item[alive-assume-time]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'alive-assume-time' cannot be less than zero", e.getMessage());
        }
        config.setAliveAssumeTime(0L);
        Assertions.assertEquals(0L, config.getAliveAssumeTime());
        config.setAliveAssumeTime(3000L);
        Assertions.assertEquals(3000L, config.getAliveAssumeTime());

        //timerCheckInterval
        try {
            config.setIntervalToClearTimeout(-1L);
            fail("Setting test failed on configuration item[timer-check-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'timer-check-interval' must be greater than zero", e.getMessage());
        }
        try {
            config.setIntervalToClearTimeout(0L);
            fail("Setting test failed on configuration item[timer-check-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'timer-check-interval' must be greater than zero", e.getMessage());
        }
        config.setIntervalToClearTimeout(3000L);
        Assertions.assertEquals(3000L, config.getIntervalToClearTimeout());

        //forceCloseUsingOnClose
        config.setForceRecycleBorrowedOnClose(true);
        Assertions.assertTrue(config.isForceRecycleBorrowedOnClose());

        //delayTimeForNextClear
        try {
            config.setParkTimeForRetry(-1L);
            fail("Setting test failed on configuration item[park-time-for-retry]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'park-time-for-retry' cannot be less than zero", e.getMessage());
        }
        config.setParkTimeForRetry(3000L);
        Assertions.assertEquals(3000L, config.getParkTimeForRetry());


        //object factory
        config.setObjectFactoryClassName(JavaBookFactory.class.getName());
        Assertions.assertEquals(JavaBookFactory.class.getName(), config.getObjectFactoryClassName());
        config.setObjectFactoryClass(JavaBookFactory.class);
        Assertions.assertEquals(JavaBookFactory.class, config.getObjectFactoryClass());
        JavaBookFactory factory = new JavaBookFactory();
        config.setObjectFactory(factory);
        Assertions.assertEquals(factory, config.getObjectFactory());

        //enableJmx
        config.setRegisterMbeans(true);
        Assertions.assertTrue(config.isRegisterMbeans());

        //printConfigInfo
        config.setPrintConfiguration(true);
        Assertions.assertTrue(config.isPrintConfiguration());

        //printRuntimeLog
        config.setPrintRuntimeLogs(true);
        Assertions.assertTrue(config.isPrintRuntimeLogs());

        //printRuntimeLog
        config.setUseThreadLocal(true);
        Assertions.assertTrue(config.isUseThreadLocal());

        //poolImplementClassName
        config.setPoolImplementClassName(null);
        Assertions.assertNull(config.getPoolImplementClassName());
        config.setPoolImplementClassName("org.stone.beeop.pool.KeyedObjectPool");
        Assertions.assertEquals("org.stone.beeop.pool.KeyedObjectPool", config.getPoolImplementClassName());
    }
}
