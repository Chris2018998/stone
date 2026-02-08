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
import org.stone.test.beeop.objects.book.BookBorrowInfo;
import org.stone.test.beeop.objects.book.JournalBook;
import org.stone.test.beeop.objects.book.TextBook;

import java.util.Properties;

/**
 * @author Chris Liao
 */
public class Tc0008ObjectInterfacesTest {

    @Test
    public void testOnSetGet() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
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
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createEmpty();
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
    public void testCheckPassed() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig<String, Book> config2 = config.check();
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
        config.setObjectInterfaceNames(new String[]{Book.class.getName(), BookBorrowInfo.class.getName()});
        config2 = config.check();
        Assertions.assertNotNull(config2.getObjectInterfaces());
    }

    @Test
    public void testCheckFailure() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
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
            Assertions.assertTrue(message != null && message.contains("Not found interface,index:0"));
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
        config.setObjectInterfaces(new Class[]{Book.class});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Not found default constructor in super class"));
        }

        config = OsConfigFactory.createDefault();
        config.setObjectInterfaces(new Class[]{Book.class, TextBook.class, JournalBook.class});
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("The count of super class cannot be greater than 1"));
        }
    }
}



