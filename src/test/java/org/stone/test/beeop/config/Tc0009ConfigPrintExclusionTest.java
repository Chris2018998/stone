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
import org.stone.test.base.TestUtil;

import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0009ConfigPrintExclusionTest {

    @Test
    public void testOnSetAndGet() throws Exception {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        Assertions.assertNull(TestUtil.getFieldValue(config, "configPrintExclusionList"));
        Assertions.assertFalse(config.existConfigPrintExclusion("initialSize"));
        Assertions.assertFalse(config.removeConfigPrintExclusion("initialSize"));
        config.clearAllConfigPrintExclusion();

        config.addConfigPrintExclusion("initialSize");
        Assertions.assertNotNull(TestUtil.getFieldValue(config, "configPrintExclusionList"));
        Assertions.assertTrue(config.existConfigPrintExclusion("initialSize"));
        Assertions.assertTrue(config.removeConfigPrintExclusion("initialSize"));

        Assertions.assertFalse(config.existConfigPrintExclusion("maxActive"));
        Assertions.assertFalse(config.removeConfigPrintExclusion("maxActive"));
        config.clearAllConfigPrintExclusion();
        config.addConfigPrintExclusion("maxActive");
        config.addConfigPrintExclusion("maxActive");
        Assertions.assertTrue(config.existConfigPrintExclusion("maxActive"));
        Assertions.assertTrue(config.removeConfigPrintExclusion("maxActive"));
    }

    @Test
    public void testOnLoadFromProperties() {
        Properties prop = new Properties();
        prop.put("configPrintExclusionList", "username,password,poolName");

        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        config.loadFromProperties(prop);
        Assertions.assertTrue(config.existConfigPrintExclusion("username"));
        Assertions.assertTrue(config.existConfigPrintExclusion("password"));
        Assertions.assertTrue(config.existConfigPrintExclusion("poolName"));
    }

    @Test
    public void testOnConfigCopy() throws Exception {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assertions.assertNull(TestUtil.getFieldValue(config2, "configPrintExclusionList"));

        config = OsConfigFactory.createDefault();
        config.addConfigPrintExclusion("poolName");
        config2 = config.check();
        Assertions.assertNotNull(TestUtil.getFieldValue(config2, "configPrintExclusionList"));

        config.removeConfigPrintExclusion("poolName");
        config2 = config.check();
        Assertions.assertNull(TestUtil.getFieldValue(config2, "configPrintExclusionList"));
    }
}
