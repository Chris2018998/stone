/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;

import java.security.InvalidParameterException;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * time configuration items value set/get
 *
 * @author Chris Liao
 */

public class Tc0002TimeControlTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        //maxWait
        Assertions.assertEquals(8000L, config.getMaxWait());//default value check
        config.setMaxWait(5000L);//positive long number is acceptable
        Assertions.assertEquals(5000L, config.getMaxWait());//check value setting

        try {
            config.setMaxWait(0L);//zero is not acceptable
            fail("[testConfigurationSet]Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxWait(-1L);//negative number is not acceptable
            fail("[testConfigurationSet]Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getMaxWait());//check field value not changed

        //idleTimeout
        Assertions.assertEquals(MINUTES.toMillis(3L), config.getIdleTimeout());//default value check
        config.setIdleTimeout(5000L);//positive long number is acceptable
        Assertions.assertEquals(5000L, config.getIdleTimeout());
        try {
            config.setIdleTimeout(0L);
            fail("[testConfigurationSet]Setting test failed on configuration item[idle-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'idle-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setIdleTimeout(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[idle-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'idle-timeout' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(5000L, config.getIdleTimeout());//check field value not changed

        //holdTimeout
        Assertions.assertEquals(0L, config.getHoldTimeout());//default value check
        config.setHoldTimeout(5000L);//positive long number is acceptable
        Assertions.assertEquals(5000L, config.getHoldTimeout());
        config.setHoldTimeout(0L);//zero is acceptable
        Assertions.assertEquals(0L, config.getHoldTimeout());
        try {
            config.setHoldTimeout(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[hold-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'hold-timeout' cannot be less than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getHoldTimeout());//check field value not changed

        //aliveTestTimeout
        Assertions.assertEquals(3, config.getAliveTestTimeout());//default value check(3 seconds)
        config.setAliveTestTimeout(0);//zero can be acceptable
        Assertions.assertEquals(0, config.getAliveTestTimeout());
        try {
            config.setAliveTestTimeout(-1);
            fail("[testConfigurationSet]Setting test failed on configuration item[alive-test-timeout]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'alive-test-timeout' cannot  be less than zero", e.getMessage());
        }
        Assertions.assertEquals(0, config.getAliveTestTimeout());//not change check


        //aliveAssumeTime
        Assertions.assertEquals(500L, config.getAliveAssumeTime());//default value check(500L mill-seconds)
        config.setAliveAssumeTime(0L);
        Assertions.assertEquals(0L, config.getAliveAssumeTime());
        try {
            config.setAliveAssumeTime(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[alive-assume-time]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'alive-assume-time' cannot be less than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getAliveAssumeTime());//not change check

        //timerCheckInterval
        Assertions.assertEquals(MINUTES.toMillis(3L), config.getTimerCheckInterval());//default value check(3 minutes)
        config.setAliveAssumeTime(MINUTES.toMillis(2L));
        Assertions.assertEquals(MINUTES.toMillis(2L), config.getAliveAssumeTime());
        try {
            config.setTimerCheckInterval(0L);
            fail("[testConfigurationSet]Setting test failed on configuration item[timer-check-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'timer-check-interval' must be greater than zero", e.getMessage());
        }
        try {
            config.setTimerCheckInterval(-1L);
            fail("[testConfigurationSet]Setting test failed on configuration item[timer-check-interval]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'timer-check-interval' must be greater than zero", e.getMessage());
        }
        Assertions.assertEquals(MINUTES.toMillis(2L), config.getAliveAssumeTime());//not change check


        //forceCloseUsingOnClear
        Assertions.assertFalse(config.isForceRecycleBorrowedOnClose());//default value check(false)
        config.setForceRecycleBorrowedOnClose(true);
        Assertions.assertTrue(config.isForceRecycleBorrowedOnClose());

        //delayTimeForNextClear
        Assertions.assertEquals(3000L, config.getParkTimeForRetry());//default value check(300L)
        config.setParkTimeForRetry(0);//0 can be acceptable
        Assertions.assertEquals(0L, config.getParkTimeForRetry());
        try {
            config.setParkTimeForRetry(-1);
            fail("[testConfigurationSet]Setting test failed on configuration item[park-time-for-retry]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'park-time-for-retry' cannot be less than zero", e.getMessage());
        }
        Assertions.assertEquals(0L, config.getParkTimeForRetry());//not change check

    }
}
