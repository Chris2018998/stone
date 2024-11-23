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
import org.stone.beeop.objects.JavaBookFactory;

import static org.stone.tools.CommonUtil.NCPU;

/**
 * @author Chris Liao
 */
public class Tc0003ObjectSizeTest extends TestCase {

    public void testOnSetAndGet() {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();

        config.setInitialSize(-1);
        config.setMaxActive(-1);
        Assert.assertNotEquals(-1, config.getInitialSize());
        Assert.assertNotEquals(-1, config.getMaxActive());

        config.setInitialSize(0);
        config.setMaxActive(0);
        Assert.assertEquals(0, config.getInitialSize());
        Assert.assertNotEquals(0, config.getMaxActive());

        config.setInitialSize(1);
        config.setMaxActive(1);
        Assert.assertEquals(1, config.getInitialSize());
        Assert.assertEquals(1, config.getMaxActive());
        Assert.assertEquals(1, config.getBorrowSemaphoreSize());

        config.setInitialSize(10);
        config.setMaxActive(20);
        Assert.assertEquals(10, config.getInitialSize());
        Assert.assertEquals(20, config.getMaxActive());

        int borrowSemaphoreExpectSize = Math.min(20 / 2, NCPU);
        Assert.assertEquals(config.getBorrowSemaphoreSize(), borrowSemaphoreExpectSize);
    }

    public void testOnErrorInitialSize() {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());

        config.setMaxActive(5);
        config.setInitialSize(10);

        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("initial-size must not be greater than max-active"));
        }
    }
}
