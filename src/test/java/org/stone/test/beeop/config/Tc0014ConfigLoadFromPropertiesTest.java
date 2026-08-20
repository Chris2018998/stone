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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.beeop.pool.ObjectPoolStatics.CONFIG_FACTORY_PROP_KEY_PREFIX;
import static org.stone.beeop.pool.ObjectPoolStatics.CONFIG_FACTORY_PROP_SIZE;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0014ConfigLoadFromPropertiesTest {
    //****************************************************************************************************************//
    //                                                  test on Properties                                                   //
    //****************************************************************************************************************//
    @Test
    public void testLoadFailureFromProperties() {
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createEmpty();
        try {
            config1.load((Properties) null);
            fail("[testLoadFailureFromProperties]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load properties cannot be null or empty"));
        }

        try {//correct
            config1.load(new Properties());
            fail("[testLoadFailureFromProperties]not threw exception when loading empty properties");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load properties cannot be null or empty"));
        }

        try {//correct
            Properties properties = new Properties();
            properties.put("maxActive", "oooo");
            config1.load(properties);
            fail("[testLoadFailureFromProperties]not threw exception when loading invalid properties item");
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }

    @Test
    public void testLoadSuccessFromProperties() {
        Properties prop = new Properties();
        prop.put("objectFactoryClass", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("objectFactoryClassName", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("predicateClass", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("predicateClassName", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("objectInterfaces", "org.stone.test.beeop.objects.book.Book");
        prop.put("objectInterfaceNames", "org.stone.test.beeop.objects.book.Book");
        prop.put("objectMethodNameList", "getTitle,getAuthor");
        prop.put(1L, 100L);//will be ignored
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        config.load(prop);
        Assertions.assertNotNull(config.getObjectFactoryClass());
        Assertions.assertNotNull(config.getObjectFactoryClassName());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getPredicateClassName());
        Assertions.assertNotNull(config.getObjectMethodNameList());
    }

    @Test
    public void testKeyPrefixOnPropertiesKey() {
        Properties prop = new Properties();
        prop.put("beeop1.objectFactoryClass", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("beeop1.objectFactoryClassName", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("beeop1.predicateClass", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("beeop1.predicateClassName", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("beeop1.objectInterfaces", "org.stone.test.beeop.objects.book.Book");
        prop.put("beeop1.objectInterfaceNames", "org.stone.test.beeop.objects.book.Book");
        prop.put("beeop1.objectMethodNameList", "getTitle,getAuthor");

        prop.put("beeop2.objectFactoryClass", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("beeop2.objectFactoryClassName", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("beeop2.predicateClass", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("beeop2.predicateClassName", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("beeop2.objectInterfaces", "org.stone.test.beeop.objects.book.Book");
        prop.put("beeop2.objectInterfaceNames", "org.stone.test.beeop.objects.book.Book");
        prop.put("beeop2.objectMethodNameList", "getTitle,getAuthor");

        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createEmpty();
        String prefix1 = "beeop1.";
        config1.load(prop, prefix1);
        Assertions.assertNotNull(config1.getObjectFactoryClass());
        Assertions.assertNotNull(config1.getObjectFactoryClassName());
        Assertions.assertNotNull(config1.getPredicateClass());
        Assertions.assertNotNull(config1.getPredicateClassName());
        Assertions.assertNotNull(config1.getObjectMethodNameList());

        prefix1 = "beeop1";
        config1 = OsConfigFactory.createEmpty();
        config1.load(prop, prefix1);
        Assertions.assertNotNull(config1.getObjectFactoryClass());
        Assertions.assertNotNull(config1.getObjectFactoryClassName());
        Assertions.assertNotNull(config1.getPredicateClass());
        Assertions.assertNotNull(config1.getPredicateClassName());
        Assertions.assertNotNull(config1.getObjectMethodNameList());

        String prefix2 = "beeop2.";
        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createEmpty();
        config2.load(prop, prefix2);
        Assertions.assertNotNull(config2.getObjectFactoryClass());
        Assertions.assertNotNull(config2.getObjectFactoryClassName());
        Assertions.assertNotNull(config2.getPredicateClass());
        Assertions.assertNotNull(config2.getPredicateClassName());
        Assertions.assertNotNull(config2.getObjectMethodNameList());
    }

    //****************************************************************************************************************//
    //                                                  test on map                                                   //
    //****************************************************************************************************************//
    @Test
    public void testLoadFailureFromMap() {
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createEmpty();
        try {
            config1.load((Map<String, Object>) null);
            fail("[testLoadFailureFromMap]not threw exception when loading null properties file");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load map cannot be null or empty"));
        }

        try {//correct
            config1.load(new HashMap<>());
            fail("[testLoadFailureFromMap]not threw exception when loading empty properties");
        } catch (Exception e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Load map cannot be null or empty"));
        }

        try {//correct
            Map<String, Object> map = new HashMap<>();
            map.put("maxActive", "oooo");
            config1.load(map);
            fail("[testLoadFailureFromMap]not threw exception when loading invalid properties item");
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to convert value[oooo]to property type(maxActive:int)"));
        }
    }

    @Test
    public void testObjectFactoryConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put(CONFIG_FACTORY_PROP_SIZE, "3");
        map.put(CONFIG_FACTORY_PROP_KEY_PREFIX + 1, "username=root&password=root");
        map.put(CONFIG_FACTORY_PROP_KEY_PREFIX + 2, "parkNanos=1000");
        map.put(CONFIG_FACTORY_PROP_KEY_PREFIX + 3, 10);
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createEmpty();
        config1.load(map);
        Assertions.assertEquals("root", config1.getObjectFactoryProperty("username"));
        Assertions.assertEquals("root", config1.getObjectFactoryProperty("password"));
        Assertions.assertEquals("1000", config1.getObjectFactoryProperty("parkNanos"));

        map.put(CONFIG_FACTORY_PROP_SIZE, 3);
        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createEmpty();
        config2.load(map);
        Assertions.assertEquals("root", config2.getObjectFactoryProperty("username"));
        Assertions.assertEquals("root", config2.getObjectFactoryProperty("password"));
        Assertions.assertEquals("1000", config2.getObjectFactoryProperty("parkNanos"));
    }


}
