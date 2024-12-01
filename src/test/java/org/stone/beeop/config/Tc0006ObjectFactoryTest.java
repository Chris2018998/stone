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
import org.stone.beeop.BeeObjectSourceConfigException;
import org.stone.beeop.objects.JavaBookFactory;
import org.stone.beeop.objects.JavaBookFactory2;
import org.stone.beeop.objects.JavaBookFactory3;

import java.util.Map;
import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0006ObjectFactoryTest extends TestCase {

    public void testOnAddProperty() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addFactoryProperty(null, null);
        Assert.assertNull(config.getFactoryProperty(null));
        config.addFactoryProperty(null, "value");
        Assert.assertNull(config.getFactoryProperty(null));
        config.addFactoryProperty("key", null);
        Assert.assertNull(config.getFactoryProperty(null));
        config.addFactoryProperty("key", "value");
        Assert.assertNotNull(config.getFactoryProperty("key"));
    }

    public void testOnRemoval() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addFactoryProperty("prop1", "value1");
        Assert.assertEquals("value1", config.getFactoryProperty("prop1"));
        Assert.assertEquals("value1", config.removeFactoryProperty("prop1"));
        Assert.assertNull(config.getFactoryProperty("prop1"));
    }

    //prop1=value&prop2=value2&prop3=value3
    public void testOnAddTextProperty1() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addFactoryProperty("prop1=value1&prop2=value2&prop3=value3");

        Assert.assertEquals("value1", config.getFactoryProperty("prop1"));
        Assert.assertEquals("value2", config.getFactoryProperty("prop2"));
        Assert.assertEquals("value3", config.getFactoryProperty("prop3"));
    }

    //prop1:value&prop2:value2&prop3:value3
    public void testOnAddTextProperty2() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addFactoryProperty("prop1:value1&prop2:value2&prop3:value3&prop4:value4:value5");

        Assert.assertEquals("value1", config.getFactoryProperty("prop1"));
        Assert.assertEquals("value2", config.getFactoryProperty("prop2"));
        Assert.assertEquals("value3", config.getFactoryProperty("prop3"));
        Assert.assertNull("value4", config.getFactoryProperty("prop4"));
    }

    public void testLoadFromProperties() {
        BeeObjectSourceConfig config1 = OsConfigFactory.createEmpty();
        Properties prop1 = new Properties();
        prop1.setProperty("factoryProperties", "prop1=value1&prop2=value2&prop3=value3");
        config1.loadFromProperties(prop1);
        Assert.assertEquals("value1", config1.getFactoryProperty("prop1"));
        Assert.assertEquals("value2", config1.getFactoryProperty("prop2"));
        Assert.assertEquals("value3", config1.getFactoryProperty("prop3"));

        BeeObjectSourceConfig config2 = OsConfigFactory.createEmpty();
        Properties prop2 = new Properties();
        prop2.setProperty("factoryProperties", "prop1:value1&prop2:value2&prop3:value3");
        config2.loadFromProperties(prop2);
        Assert.assertEquals("value1", config2.getFactoryProperty("prop1"));
        Assert.assertEquals("value2", config2.getFactoryProperty("prop2"));
        Assert.assertEquals("value3", config2.getFactoryProperty("prop3"));

        BeeObjectSourceConfig config3 = OsConfigFactory.createEmpty();
        Properties prop3 = new Properties();
        prop3.setProperty("factoryProperties.size", "3");
        prop3.setProperty("factoryProperties.1", "prop1=value1");
        prop3.setProperty("factoryProperties.2", "prop2:value2");
        prop3.setProperty("factoryProperties.3", "prop3=value3");
        config3.loadFromProperties(prop3);
        Assert.assertEquals("value1", config3.getFactoryProperty("prop1"));
        Assert.assertEquals("value2", config3.getFactoryProperty("prop2"));
        Assert.assertEquals("value3", config3.getFactoryProperty("prop3"));
    }

    public void testFactoryCreation() throws Exception {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assert.assertNotNull(config2.getObjectFactory());
        Assert.assertTrue(((Map) TestUtil.getFieldValue(config2, "factoryProperties")).isEmpty());

        config = OsConfigFactory.createDefault();
        config.addFactoryProperty("name", "Java");
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectFactory());
        Assert.assertFalse(((Map) TestUtil.getFieldValue(config2, "factoryProperties")).isEmpty());

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClass(JavaBookFactory.class);
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectFactory());

        config = OsConfigFactory.createEmpty();
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName"));
        }

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClassName(JavaBookFactory2.class.getName());
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Object factory must provide a non null default pooled key"));
        }

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClassName(JavaBookFactory.class.getName() + "Test");
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Not found object factory class:"));
        }

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClassName(JavaBookFactory3.class.getName());
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Failed to create object factory by class:"));
        }

        config = OsConfigFactory.createDefault();
        config.addFactoryProperty("price", "ABC");
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Failed to convert value"));
        }
    }
}
