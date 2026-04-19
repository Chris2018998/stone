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
import org.stone.test.beeop.objects.book.Book;

import java.util.Properties;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class Tc0014ConfigLoadFromPropertiesTest {

    @Test
    public void testObjectTypeFields() {
        Properties prop = new Properties();
        prop.put("objectFactoryClass", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("objectFactoryClassName", "org.stone.test.beeop.objects.factory.TextBookFactory");
        prop.put("predicateClass", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("predicateClassName", "org.stone.test.beeop.objects.predicate.JavaBookPredicate");
        prop.put("objectInterfaces", "org.stone.test.beeop.objects.book.Book");
        prop.put("objectInterfaceNames", "org.stone.test.beeop.objects.book.Book");
        prop.put("objectMethodNameList", "getTitle,getAuthor");

        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
        config.loadFromProperties(prop);
        Assertions.assertNotNull(config.getObjectFactoryClass());
        Assertions.assertNotNull(config.getObjectFactoryClassName());
        Assertions.assertNotNull(config.getPredicateClass());
        Assertions.assertNotNull(config.getPredicateClassName());
        Assertions.assertNotNull(config.getObjectMethodNameList());
    }

    @Test
    public void testKeyPrefix() {
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
        config1.loadFromProperties(prop, prefix1);
        Assertions.assertNotNull(config1.getObjectFactoryClass());
        Assertions.assertNotNull(config1.getObjectFactoryClassName());
        Assertions.assertNotNull(config1.getPredicateClass());
        Assertions.assertNotNull(config1.getPredicateClassName());
        Assertions.assertNotNull(config1.getObjectMethodNameList());

        prefix1 = "beeop1";
        config1 = OsConfigFactory.createEmpty();
        config1.loadFromProperties(prop, prefix1);
        Assertions.assertNotNull(config1.getObjectFactoryClass());
        Assertions.assertNotNull(config1.getObjectFactoryClassName());
        Assertions.assertNotNull(config1.getPredicateClass());
        Assertions.assertNotNull(config1.getPredicateClassName());
        Assertions.assertNotNull(config1.getObjectMethodNameList());

        String prefix2 = "beeop2.";
        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createEmpty();
        config2.loadFromProperties(prop, prefix2);
        Assertions.assertNotNull(config2.getObjectFactoryClass());
        Assertions.assertNotNull(config2.getObjectFactoryClassName());
        Assertions.assertNotNull(config2.getPredicateClass());
        Assertions.assertNotNull(config2.getPredicateClassName());
        Assertions.assertNotNull(config2.getObjectMethodNameList());
    }
}
