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

/**
 * @author Chris Liao
 */
public class Tc0005FactoryPropertiesTest extends TestCase {

    public void testOnSetAndGet() {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        Assert.assertNull(config.getFactoryProperty("name"));
        config.addFactoryProperty("name", "objectFactory");
        Assert.assertEquals("objectFactory", config.getFactoryProperty("name"));
        config.removeFactoryProperty("name");
        Assert.assertNull("objectFactory", config.getFactoryProperty("name"));

        config.addFactoryProperty(null, "objectFactory");
        Assert.assertNull(config.getFactoryProperty(null));
        config.addFactoryProperty("name", null);
        Assert.assertNull(config.getFactoryProperty("name"));

        config.addFactoryProperty("name=objectFactory&host=localhost");
        config.addFactoryProperty("db:mysql&locale:zh_cn");
        config.addFactoryProperty("cup:i7-14650:2G&memory:kings:32G");

        Assert.assertEquals("objectFactory", config.getFactoryProperty("name"));
        Assert.assertEquals("localhost", config.getFactoryProperty("host"));
        Assert.assertEquals("mysql", config.getFactoryProperty("db"));
        Assert.assertEquals("zh_cn", config.getFactoryProperty("locale"));
        Assert.assertNull(config.getFactoryProperty("cup"));
    }
}
