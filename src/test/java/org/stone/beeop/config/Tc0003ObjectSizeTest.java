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
import org.stone.beeop.BeeObjectSourceConfigException;

import java.security.InvalidParameterException;

import static org.stone.tools.CommonUtil.NCPU;

/**
 * @author Chris Liao
 */
public class Tc0003ObjectSizeTest extends TestCase {

    public void testOnSetAndGet() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();

        //MaxKeySize
        try {
            config.setMaxKeySize(-1);
            fail("Setting test failed on configuration item[max-key-size]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[max-key-size] must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxKeySize(0);
            fail("Setting test failed on configuration item[max-key-size]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[max-key-size] must be greater than zero", e.getMessage());
        }
        config.setMaxKeySize(10);
        Assert.assertEquals(10, config.getMaxKeySize());

        //InitialSize
        try {
            config.setInitialSize(-1);
            fail("Setting test failed on configuration item[initial-size]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[initial-size] can't be less than zero", e.getMessage());
        }
        config.setInitialSize(0);
        Assert.assertEquals(0, config.getInitialSize());
        config.setInitialSize(1);
        Assert.assertEquals(1, config.getInitialSize());

        //MaxActive
        try {
            config.setMaxActive(-1);
            fail("Setting test failed on configuration item[max-active]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[max-active] must be greater than zero", e.getMessage());
        }
        try {
            config.setMaxActive(0);
            fail("Setting test failed on configuration item[max-active]");
        } catch (InvalidParameterException e) {
            Assert.assertEquals("The given value to configuration item[max-active] must be greater than zero", e.getMessage());
        }
        config.setMaxActive(1);
        Assert.assertEquals(1, config.getMaxActive());
        Assert.assertEquals(1, config.getBorrowSemaphoreSize());
        config.setMaxActive(20);
        Assert.assertEquals(20, config.getMaxActive());
        int borrowSemaphoreExpectSize = Math.min(20 / 2, NCPU);
        Assert.assertEquals(config.getBorrowSemaphoreSize(), borrowSemaphoreExpectSize);
    }

    public void testOnErrorInitialSize() {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();

        config.setMaxActive(5);
        config.setInitialSize(10);

        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("The configured value of item[initial-size] can't be greater than the configured value of item[max-active]"));
        }
    }
}
