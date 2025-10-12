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
        Assertions.assertNull(TestUtil.getFieldValue(config, "exclusionListOfPrint"));
        Assertions.assertFalse(config.existExclusionNameOfPrint("initialSize"));
        Assertions.assertFalse(config.removeExclusionNameOfPrint("initialSize"));
        config.clearExclusionListOfPrint();

        config.addExclusionNameOfPrint("initialSize");
        Assertions.assertNotNull(TestUtil.getFieldValue(config, "exclusionListOfPrint"));
        Assertions.assertTrue(config.existExclusionNameOfPrint("initialSize"));
        Assertions.assertTrue(config.removeExclusionNameOfPrint("initialSize"));

        Assertions.assertFalse(config.existExclusionNameOfPrint("maxActive"));
        Assertions.assertFalse(config.removeExclusionNameOfPrint("maxActive"));
        config.clearExclusionListOfPrint();
        config.addExclusionNameOfPrint("maxActive");
        config.addExclusionNameOfPrint("maxActive");
        Assertions.assertTrue(config.existExclusionNameOfPrint("maxActive"));
        Assertions.assertTrue(config.removeExclusionNameOfPrint("maxActive"));
    }

    @Test
    public void testOnLoadFromProperties() {
        Properties prop = new Properties();
        prop.put("exclusionListOfPrint", "username,password,poolName");

        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        config.loadFromProperties(prop);
        Assertions.assertTrue(config.existExclusionNameOfPrint("username"));
        Assertions.assertTrue(config.existExclusionNameOfPrint("password"));
        Assertions.assertTrue(config.existExclusionNameOfPrint("poolName"));
    }

    @Test
    public void testOnConfigCopy() throws Exception {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assertions.assertNull(TestUtil.getFieldValue(config2, "exclusionListOfPrint"));

        config = OsConfigFactory.createDefault();
        config.addExclusionNameOfPrint("poolName");
        config2 = config.check();
        Assertions.assertNotNull(TestUtil.getFieldValue(config2, "exclusionListOfPrint"));

        config.removeExclusionNameOfPrint("poolName");
        config2 = config.check();
        Assertions.assertNull(TestUtil.getFieldValue(config2, "exclusionListOfPrint"));
    }
}
