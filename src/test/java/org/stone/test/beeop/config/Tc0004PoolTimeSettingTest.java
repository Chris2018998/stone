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
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.test.beeop.objects.book.Book;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0004PoolTimeSettingTest {

    @Test
    public void testSetAndGet() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setMaxWait(5000L);
        Assertions.assertEquals(5000L, config.getMaxWait());

        //maxWait
        try {
            config.setMaxWait(-1L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'max-wait' must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxWait(0L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'max-wait' must be greater than zero", e.getMessage());
        }


        //idleTimeout
        try {
            config.setIdleTimeout(-1L);
            fail("Setting test failed on configuration item[idle-timeout]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'idle-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setIdleTimeout(0L);
            fail("Setting test failed on configuration item[idle-timeout]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'idle-timeout' must be greater than zero", e.getMessage());
        }
        config.setIdleTimeout(3000L);
        Assertions.assertEquals(3000L, config.getIdleTimeout());

        //holdTimeout
        try {
            config.setHoldTimeout(-1L);
            fail("Setting test failed on configuration item[hold-timeout]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'hold-timeout' cannot be less than zero", e.getMessage());
        }
        config.setHoldTimeout(0);
        Assertions.assertEquals(0, config.getHoldTimeout());
        config.setHoldTimeout(3000L);
        Assertions.assertEquals(3000L, config.getHoldTimeout());

        //aliveTestTimeout
        try {
            config.setAliveTestTimeout(-1);
            fail("Setting test failed on configuration item[alive-test-timeout]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'alive-test-timeout' cannot  be less than zero", e.getMessage());
        }
        config.setAliveTestTimeout(0);
        Assertions.assertEquals(0, config.getAliveTestTimeout());
        config.setAliveTestTimeout(3);
        Assertions.assertEquals(3, config.getAliveTestTimeout());

        //aliveAssumeTime
        try {
            config.setAliveAssumeTime(-1L);
            fail("Setting test failed on configuration item[alive-assume-time]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'alive-assume-time' cannot be less than zero", e.getMessage());
        }
        config.setAliveAssumeTime(0L);
        Assertions.assertEquals(0L, config.getAliveAssumeTime());
        config.setAliveAssumeTime(3000L);
        Assertions.assertEquals(3000L, config.getAliveAssumeTime());

        //timerCheckInterval
        try {
            config.setIntervalOfClearTimeout(-1L);
            fail("Setting test failed on configuration item[timer-check-interval]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'interval-of-clear-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setIntervalOfClearTimeout(0L);
            fail("Setting test failed on configuration item[timer-check-interval]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'interval-of-clear-timeout' must be greater than zero", e.getMessage());
        }
        config.setIntervalOfClearTimeout(3000L);
        Assertions.assertEquals(3000L, config.getIntervalOfClearTimeout());

        //forceCloseUsingOnClose
        config.setForceRecycleBorrowedOnClose(true);
        Assertions.assertTrue(config.isForceRecycleBorrowedOnClose());

        //delayTimeForNextClear
        try {
            config.setParkTimeForRetry(-1L);
            fail("Setting test failed on configuration item[park-time-for-retry]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'park-time-for-retry' cannot be less than zero", e.getMessage());
        }
        config.setParkTimeForRetry(3000L);
        Assertions.assertEquals(3000L, config.getParkTimeForRetry());
    }
}
