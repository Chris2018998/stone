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
import org.stone.beeop.BeeObjectSourceConfigException;
import org.stone.beeop.objects.Book;
import org.stone.beeop.objects.BookMarket;

import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0005ObjectInterfacesTest extends TestCase {

    public void testOnSetGet() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        //object interfaces
        Class<?>[] interfaces = new Class[]{Book.class};
        String[] interfaceNames = new String[]{Book.class.getName()};
        config.setObjectInterfaces(interfaces);
        config.setObjectInterfaceNames(interfaceNames);
        for (String name : config.getObjectInterfaceNames())
            Assert.assertEquals(name, Book.class.getName());
        for (Class<?> oInterface : config.getObjectInterfaces())
            Assert.assertEquals(Book.class, oInterface);
    }

    public void testLoadFromProperties() {
        Properties prop = new Properties();
        prop.put("objectInterfaces", Book.class.getName());
        prop.put("objectInterfaceNames", Book.class.getName());
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.loadFromProperties(prop);
        for (String name : config.getObjectInterfaceNames())
            Assert.assertEquals(name, Book.class.getName());
        for (Class<?> oInterface : config.getObjectInterfaces())
            Assert.assertEquals(Book.class, oInterface);

        prop = new Properties();
        prop.put("objectInterfaces", Book.class.getName() + "Test");
        try {
            config.loadFromProperties(prop);
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Class not found:"));
        }
    }

    public void testOnCheck() {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assert.assertNull(config2.getObjectInterfaces());
        Assert.assertNull(config2.getObjectInterfaceNames());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[0]);
        config2 = config.check();
        Assert.assertNull(config2.getObjectInterfaces());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[0]);
        config2 = config.check();
        Assert.assertNull(config2.getObjectInterfaces());

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[]{Book.class});
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectInterfaces());

        config=OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{Book.class.getName()});
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectInterfaces());
        config=OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{Book.class.getName(), BookMarket.class.getName()});
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectInterfaces());


        config.setObjectInterfaces(new Class[]{null});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Object interfaces[0]is null"));
        }

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[]{String.class});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Object interfaces[0]is not a valid interface"));
        }


        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{null});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Object interface class names[0]is empty or null"));
        }

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaceNames(new String[]{Book.class.getName() + "Test"});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Not found interface class with class names[0]"));
        }
    }
}



