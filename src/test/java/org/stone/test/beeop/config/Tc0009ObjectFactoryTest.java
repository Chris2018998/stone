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
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.factory.TextBookFactory2;
import org.stone.tools.exception.BeanException;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0009ObjectFactoryTest {

    @Test
    public void testSetting() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        TextBookFactory factory = new TextBookFactory();
        config.setObjectFactory(factory);
        Assertions.assertEquals(factory, config.getObjectFactory());

        config.setObjectFactoryClass(TextBookFactory.class);
        Assertions.assertEquals(TextBookFactory.class, config.getObjectFactoryClass());

        config.setObjectFactoryClassName(TextBookFactory.class.getName());
        Assertions.assertEquals(TextBookFactory.class.getName(), config.getObjectFactoryClassName());
    }

    @Test
    public void testCheckPass() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        TextBookFactory factory = new TextBookFactory();
        config.setObjectFactory(factory);
        BeeObjectSourceConfig<String, Book> checkedConfig = config.check();
        Assertions.assertEquals(factory, checkedConfig.getObjectFactory());
        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClass(TextBookFactory.class);
        checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getObjectFactory());
        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClassName(TextBookFactory.class.getName());
        checkedConfig = config.check();
        Assertions.assertNotNull(checkedConfig.getObjectFactory());
    }

    @Test
    public void testFactoryPropertiesInjection() {
        //test1:
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createEmpty();
        config1.setObjectFactoryClassName(TextBookFactory.class.getName());
        config1.addObjectFactoryProperty("factoryName", "Tech books of Computer");
        BeeObjectSourceConfig<String, Book> newConfig = config1.check();
        Assertions.assertInstanceOf(TextBookFactory.class, newConfig.getObjectFactory());
        TextBookFactory bookFactory = (TextBookFactory) newConfig.getObjectFactory();
        Assertions.assertEquals("Tech books of Computer", bookFactory.getFactoryName());

        //test2:
        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createEmpty();
        config2.setObjectFactoryClassName(TextBookFactory.class.getName());
        config2.addObjectFactoryProperty("factoryName=Tech books of Computer&factoryCountry=China");
        BeeObjectSourceConfig<String, Book> newConfig2 = config2.check();
        Assertions.assertInstanceOf(TextBookFactory.class, newConfig2.getObjectFactory());
        TextBookFactory bookFactory2 = (TextBookFactory) newConfig2.getObjectFactory();
        Assertions.assertEquals("Tech books of Computer", bookFactory2.getFactoryName());
        Assertions.assertEquals("China", bookFactory2.getFactoryCountry());

        config2.removeObjectFactoryProperty("factoryName");
        config2.removeObjectFactoryProperty("factoryCountry");
        BeeObjectSourceConfig<String, Book> newConfig3 = config2.check();
        TextBookFactory bookFactory3 = (TextBookFactory) newConfig3.getObjectFactory();
        Assertions.assertNull(bookFactory3.getFactoryName());
        Assertions.assertNull(bookFactory3.getFactoryCountry());
    }

    @Test
    public void testCheckFailure() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        try {
            config.check();
            fail("Setting test failed on configuration item[object-factory]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]", e.getMessage());
        }

        //Check null value on default key
        TextBookFactory factory = new TextBookFactory();
        factory.setDefaultKey(null);
        config.setObjectFactory(factory);
        try {
            config.check();
            fail("Setting test failed on configuration item[object-factory]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("Object factory must provide a non null default pooled key", e.getMessage());
        }

        //Check factory class not found
        config = OsConfigFactory.createEmpty();
        String factoryClassName = TextBookFactory.class.getName() + "NotFound";
        config.setObjectFactoryClassName(factoryClassName);
        try {
            config.check();
            fail("Setting test failed on configuration item[object-factory]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertEquals("Not found object factory class:" + factoryClassName, e.getMessage());
        }

        //factory class no constructor
        config = OsConfigFactory.createEmpty();
        factoryClassName = TextBookFactory2.class.getName();
        config.setObjectFactoryClassName(factoryClassName);
        try {
            config.check();
            fail("Setting test failed on configuration item[object-factory]");
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to create object factory by class:"));
        }

        //incorrect factory properties configuration
        config = OsConfigFactory.createEmpty();
        config.setObjectFactoryClass(TextBookFactory.class);
        config.addObjectFactoryProperty("nullResultFlag2", "false");//<!--injection skip invalid property
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            Assertions.assertInstanceOf(BeanException.class, e.getCause());
        }
    }

    @Test
    public void testOnAddProperty() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
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
    public void testFactoryPropertiesRemoval() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        Assertions.assertNull(config.removeObjectFactoryProperty("prop1"));
        config.addObjectFactoryProperty("prop1", "value1");
        Assertions.assertEquals("value1", config.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value1", config.removeObjectFactoryProperty("prop1"));
        Assertions.assertNull(config.getObjectFactoryProperty("prop1"));
    }

    @Test
    //prop1=value&prop2=value2&prop3=value3
    public void testOnAddTextProperty1() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        config.addObjectFactoryProperty("prop1=value1&prop2=value2&prop3=value3");

        Assertions.assertEquals("value1", config.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config.getObjectFactoryProperty("prop3"));
    }

    @Test
    //prop1:value&prop2:value2&prop3:value3
    public void testOnAddTextProperty2() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        config.addObjectFactoryProperty("prop1:value1&prop2:value2&prop3:value3&prop4:value4:value5");

        Assertions.assertEquals("value1", config.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config.getObjectFactoryProperty("prop3"));
        Assertions.assertNull(config.getObjectFactoryProperty("prop4"));
    }

    @Test
    public void testLoadFromProperties() {
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createEmpty();
        Properties prop1 = new Properties();
        prop1.setProperty("objectFactoryProperties", "prop1=value1&prop2=value2&prop3=value3");
        config1.loadFromProperties(prop1);
        Assertions.assertEquals("value1", config1.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config1.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config1.getObjectFactoryProperty("prop3"));

        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createEmpty();
        Properties prop2 = new Properties();
        prop2.setProperty("objectFactoryProperties", "prop1:value1&prop2:value2&prop3:value3");
        config2.loadFromProperties(prop2);
        Assertions.assertEquals("value1", config2.getObjectFactoryProperty("prop1"));
        Assertions.assertEquals("value2", config2.getObjectFactoryProperty("prop2"));
        Assertions.assertEquals("value3", config2.getObjectFactoryProperty("prop3"));

        BeeObjectSourceConfig<String, Book> config3 = OsConfigFactory.createEmpty();
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
}
