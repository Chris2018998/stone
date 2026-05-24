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
public class Tc0010MethodLogCacheTest {
    @Test
    public void testSetAndGet() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        //enableLogCache
        config.setEnableLogCache(true);
        Assertions.assertTrue(config.isEnableLogCache());
        config.setEnableLogCache(false);
        Assertions.assertFalse(config.isEnableLogCache());

        //logCacheSize
        config.setLogCacheSize(100);
        Assertions.assertEquals(100, config.getLogCacheSize());
        try {
            config.setLogCacheSize(-1);
            fail("Setting test failed on configuration item[log-cache-size]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'log-cache-size' must be greater than zero", e.getMessage());
        }
        try {
            config.setLogCacheSize(0);
            fail("Setting test failed on configuration item[log-cache-size]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'log-cache-size' must be greater than zero", e.getMessage());
        }


        //logTimeout
        config.setLogTimeout(360000L);
        Assertions.assertEquals(360000L, config.getLogTimeout());
        try {
            config.setLogTimeout(-1);
            fail("Setting test failed on configuration item[log-timeout]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'log-timeout' must be greater than zero", e.getMessage());
        }
        try {
            config.setLogTimeout(0);
            fail("Setting test failed on configuration item[log-timeout]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'log-timeout' must be greater than zero", e.getMessage());
        }

        //intervalOfClearTimeoutLogs
        config.setIntervalOfClearTimeoutLogs(360000L);
        Assertions.assertEquals(360000L, config.getIntervalOfClearTimeoutLogs());
        try {
            config.setIntervalOfClearTimeoutLogs(-1);
            fail("Setting test failed on configuration item[interval-of-clear-timeout-execution-logs]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'interval-of-clear-timeout-execution-logs' must be greater than zero", e.getMessage());
        }
        try {
            config.setIntervalOfClearTimeoutLogs(0);
            fail("Setting test failed on configuration item[interval-of-clear-timeout-logs]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'interval-of-clear-timeout-execution-logs' must be greater than zero", e.getMessage());
        }

        //slowGetThreshold
        try {
            config.setSlowGetThreshold(-1L);
            fail("[testSetAndGet]Setting test failed on configuration item[slow-get-threshold]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'slow-get-threshold' must be greater than zero", e.getMessage());
        }
        try {
            config.setSlowGetThreshold(0L);
            fail("[testSetAndGet]Setting test failed on configuration item[slow-get-threshold]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'slow-get-threshold' must be greater than zero", e.getMessage());
        }

        try {
            config.setSlowGetThreshold(config.getMaxWait() + 1L);
            fail("[testSetAndGet]Setting test failed on configuration item[slow-get-threshold]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'slow-get-threshold' cannot be greater than 'max-wait'", e.getMessage());
        }
        try {
            config.setSlowGetThreshold(config.getMaxWait());
        } catch (BeeObjectSourceConfigException e) {
            fail("[testSetAndGet]Setting test failed on configuration item[slow-get-threshold]");
        }

        //slowCallThreshold
        config.setSlowCallThreshold(360000L);
        Assertions.assertEquals(360000L, config.getSlowCallThreshold());
        try {
            config.setSlowCallThreshold(-1L);
            fail("Setting test failed on configuration item[interval-of-clear-timeout-logs]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'slow-call-threshold' must be greater than zero", e.getMessage());
        }
        try {
            config.setSlowCallThreshold(0L);
            fail("Setting test failed on configuration item[interval-of-clear-timeout-logs]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("The given value of 'slow-call-threshold' must be greater than zero", e.getMessage());
        }
    }
}




