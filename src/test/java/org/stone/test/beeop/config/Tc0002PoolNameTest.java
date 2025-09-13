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

import java.util.Properties;

/**
 * @author Chris Liao
 */

public class Tc0002PoolNameTest {

    @Test
    public void testOnSetGet() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();

        config.setPoolName(null);
        Assertions.assertNull(config.getPoolName());

        config.setPoolName("pool1");
        Assertions.assertEquals("pool1", config.getPoolName());
    }

    @Test
    public void testOnGeneration() {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig checkConfig = config.check();
        Assertions.assertTrue(checkConfig.getPoolName().contains("KeyPool-"));

        config.setPoolName("pool1");
        checkConfig = config.check();
        Assertions.assertEquals("pool1", checkConfig.getPoolName());
    }

    @Test
    public void testInProperties() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        Properties prop = new Properties();

        prop.setProperty("poolName", "pool1");
        config.loadFromProperties(prop);
        Assertions.assertEquals("pool1", config.getPoolName());

        prop.clear();
        prop.setProperty("pool-name", "pool2");
        config.loadFromProperties(prop);
        Assertions.assertEquals("pool2", config.getPoolName());

        prop.clear();
        prop.setProperty("pool_name", "pool3");
        config.loadFromProperties(prop);
        Assertions.assertEquals("pool3", config.getPoolName());
    }
}
