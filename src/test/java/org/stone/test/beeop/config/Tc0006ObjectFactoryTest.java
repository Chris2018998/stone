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
import org.stone.beeop.BeeObjectSourceConfigException;
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.objects.JavaBookFactory;
import org.stone.test.beeop.objects.JavaBookFactory2;
import org.stone.test.beeop.objects.JavaBookFactory3;

import java.util.Map;
import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0006ObjectFactoryTest {

    @Test
    public void testOnAddProperty() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addObjectFactoryProperty(null, null);
        Assertions.assertNull(config.getObjectFactoryProperty(null));
        config.addObjectFactoryProperty(null, "value");
        Assertions.assertNull(config.getObjectFactoryProperty(null));
        config.addObjectFactoryProperty("key", null);
        Assertions.assertNull(config.getObjectFactoryProperty(null));
        config.addObjectFactoryProperty("key", "value");
        Assertions.assertNotNull(config.getObjectFactoryProperty("key"));
    }

    @Test
    public void testOnRemoval() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addObjectFactoryProperty("prop1", "value1");
        Assertions.assertEquals("value1", config.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value1", config.removeObjectFactoryProperty("prop1"));
        Assertions.assertNull(config.getObjectFactoryProperty("prop1"));
    }

    @Test
    //prop1=value&prop2=value2&prop3=value3
    public void testOnAddTextProperty1() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addObjectFactoryProperty("prop1=value1&prop2=value2&prop3=value3");

        Assertions.assertEquals("value1", config.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config.getObjectFactoryProperty("prop3"));
    }

    @Test
    //prop1:value&prop2:value2&prop3:value3
    public void testOnAddTextProperty2() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.addObjectFactoryProperty("prop1:value1&prop2:value2&prop3:value3&prop4:value4:value5");

        Assertions.assertEquals("value1", config.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config.getObjectFactoryProperty("prop3"));
        Assertions.assertNull(config.getObjectFactoryProperty("prop4"));
    }

    @Test
    public void testLoadFromProperties() {
        BeeObjectSourceConfig config1 = OsConfigFactory.createEmpty();
        Properties prop1 = new Properties();
        prop1.setProperty("objectFactoryProperties", "prop1=value1&prop2=value2&prop3=value3");
        config1.loadFromProperties(prop1);
        Assertions.assertEquals("value1", config1.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config1.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config1.getObjectFactoryProperty("prop3"));

        BeeObjectSourceConfig config2 = OsConfigFactory.createEmpty();
        Properties prop2 = new Properties();
        prop2.setProperty("objectFactoryProperties", "prop1:value1&prop2:value2&prop3:value3");
        config2.loadFromProperties(prop2);
        Assertions.assertEquals("value1", config2.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config2.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config2.getObjectFactoryProperty("prop3"));

        BeeObjectSourceConfig config3 = OsConfigFactory.createEmpty();
        Properties prop3 = new Properties();
        prop3.setProperty("objectFactoryProperties.size", "3");
        prop3.setProperty("objectFactoryProperties.1", "prop1=value1");
        prop3.setProperty("objectFactoryProperties.2", "prop2:value2");
        prop3.setProperty("objectFactoryProperties.3", "prop3=value3");
        config3.loadFromProperties(prop3);
        Assertions.assertEquals("value1", config3.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config3.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config3.getObjectFactoryProperty("prop3"));
    }

    @Test
    public void testFactoryCreation() throws Exception {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assertions.assertNotNull(config2.getObjectFactory());
        Assertions.assertTrue(((Map<?, ?>) TestUtil.getFieldValue(config2, "objectFactoryProperties")).isEmpty());

        config = OsConfigFactory.createDefault();
        config.addObjectFactoryProperty("name", "Java");
        config2 = config.check();
        Assertions.assertNotNull(config2.getObjectFactory());
        Assertions.assertFalse(((Map<?, ?>) TestUtil.getFieldValue(config2, "objectFactoryProperties")).isEmpty());

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClass(JavaBookFactory.class);
        config2 = config.check();
        Assertions.assertNotNull(config2.getObjectFactory());

        config = OsConfigFactory.createEmpty();
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName"));
        }

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClassName(JavaBookFactory2.class.getName());
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Object factory must provide a non null default pooled key"));
        }

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClassName(JavaBookFactory.class.getName() + "Test");
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Not found object factory class:"));
        }

        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClassName(JavaBookFactory3.class.getName());
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to create object factory by class:"));
        }

        config = OsConfigFactory.createDefault();
        config.addObjectFactoryProperty("price", "ABC");
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value"));
        }
    }
}
