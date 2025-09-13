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
import org.stone.test.beeop.objects.books.*;

import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0005ObjectInterfacesTest {

    @Test
    public void testOnSetGet() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        //object interfaces
        Class<?>[] interfaces = new Class[]{Book.class};
        String[] interfaceNames = new String[]{Book.class.getName()};
        config.setObjectInterfaces(interfaces);
        config.setObjectInterfaceNames(interfaceNames);
        for (String name : config.getObjectInterfaceNames())
            Assertions.assertEquals(name, Book.class.getName());
        for (Class<?> oInterface : config.getObjectInterfaces())
            Assertions.assertEquals(Book.class, oInterface);
    }

    @Test
    public void testLoadFromProperties() {
        Properties prop = new Properties();
        prop.put("objectInterfaces", Book.class.getName());
        prop.put("objectInterfaceNames", Book.class.getName());
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.loadFromProperties(prop);
        for (String name : config.getObjectInterfaceNames())
            Assertions.assertEquals(name, Book.class.getName());
        for (Class<?> oInterface : config.getObjectInterfaces())
            Assertions.assertEquals(Book.class, oInterface);

        prop = new Properties();
        prop.put("objectInterfaces", Book.class.getName() + "Test");
        try {
            config.loadFromProperties(prop);
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Class not found:"));
        }
    }

    @Test
    public void testOnCheck() {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assertions.assertNull(config2.getObjectInterfaces());
        Assertions.assertNull(config2.getObjectInterfaceNames());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[0]);
        config2 = config.check();
        Assertions.assertNull(config2.getObjectInterfaces());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[0]);
        config2 = config.check();
        Assertions.assertNull(config2.getObjectInterfaces());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[]{Book.class});
        config2 = config.check();
        Assertions.assertNotNull(config2.getObjectInterfaces());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{Book.class.getName()});
        config2 = config.check();
        Assertions.assertNotNull(config2.getObjectInterfaces());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{Book.class.getName(), BookMarket.class.getName()});
        config2 = config.check();
        Assertions.assertNotNull(config2.getObjectInterfaces());


        config.setObjectInterfaces(new Class[]{null});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Object interfaces[0]is null"));
        }

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{null});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Object interface class names[0]is empty or null"));
        }

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{Book.class.getName() + "Test"});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Not found interface class with class names[0]"));
        }


        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[]{String.class});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Object supper class cannot be final type"));
        }


        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[]{EnglishBook.class});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Not found a constructor without parameters in super class"));
        }

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[]{JavaBook.class, MathBook.class});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("The count of super class cannot be greater than 1"));
        }
    }
}



