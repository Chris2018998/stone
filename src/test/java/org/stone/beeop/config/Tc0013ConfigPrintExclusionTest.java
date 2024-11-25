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
import org.stone.base.TestUtil;
import org.stone.beeop.BeeObjectSourceConfig;

/**
 * @author Chris Liao
 */
public class Tc0013ConfigPrintExclusionTest extends TestCase {

    public void testOnSetAndGet() throws Exception {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        Assert.assertNull(TestUtil.getFieldValue(config, "configPrintExclusionList"));
        Assert.assertFalse(config.existConfigPrintExclusion("initialSize"));
        Assert.assertFalse(config.removeConfigPrintExclusion("initialSize"));
        config.clearAllConfigPrintExclusion();

        config.addConfigPrintExclusion("initialSize");
        Assert.assertNotNull(TestUtil.getFieldValue(config, "configPrintExclusionList"));
        Assert.assertTrue(config.existConfigPrintExclusion("initialSize"));
        Assert.assertTrue(config.removeConfigPrintExclusion("initialSize"));

        Assert.assertFalse(config.existConfigPrintExclusion("maxActive"));
        Assert.assertFalse(config.removeConfigPrintExclusion("maxActive"));
        config.clearAllConfigPrintExclusion();
        config.addConfigPrintExclusion("maxActive");
        config.addConfigPrintExclusion("maxActive");
        Assert.assertTrue(config.existConfigPrintExclusion("maxActive"));
        Assert.assertTrue(config.removeConfigPrintExclusion("maxActive"));
    }

}
