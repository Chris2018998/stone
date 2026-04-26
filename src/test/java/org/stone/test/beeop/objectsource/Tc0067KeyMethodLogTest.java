/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objectsource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.*;
import org.stone.beeop.exception.BeePooledObjectCreationException;
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.methodLog.LogListener1;
import org.stone.test.beeop.objects.methodLog.LogListener3;

import java.util.List;

/**
 * @author Chris Liao
 */
public class Tc0067KeyMethodLogTest {

    @Test
    public void testSuccessLog() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setEnableLogCache(true);
        BeeObjectFactory<String, Book> objectFactory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
            List<BeeMethodLog<String>> Loglist = os.getKeyLogs(objectFactory.getDefaultKey());
            Assertions.assertEquals(1, Loglist.size());
            Assertions.assertTrue(Loglist.get(0).isSuccessful());
        }
    }

    @Test
    public void testExceptionLog() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setEnableLogCache(true);
        config.setInitialSize(0);
        TextBookFactory bookFactory = new TextBookFactory();
        bookFactory.setCreationException(new Exception("Paper is not enough"));
        config.setObjectFactory(bookFactory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0067KeyMethodLogTest.testExceptionLog]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectCreationException.class, e);
                Assertions.assertEquals("Paper is not enough", e.getCause().getMessage());
            }
            List<BeeMethodLog<String>> Loglist = os.getKeyLogs(bookFactory.getDefaultKey());
            Assertions.assertEquals(1, Loglist.size());
            Assertions.assertTrue(Loglist.get(0).isException());

            os.clearKeyLogs(bookFactory.getDefaultKey());
            Assertions.assertEquals(0, os.getKeyLogs(bookFactory.getDefaultKey()).size());
        }
    }

    @Test
    public void testLogCacheEnable() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setMaxKeySize(10);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setEnableLogCache(true);//enable log cache

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
            Assertions.assertEquals(1, os.getKeyLogs(config.getObjectFactory().getDefaultKey()).size());
            os.clearKeyLogs(config.getObjectFactory().getDefaultKey());
            Assertions.assertEquals(0, os.getKeyLogs(config.getObjectFactory().getDefaultKey()).size());

            //disable LogCache
            os.enableLogCache(config.getObjectFactory().getDefaultKey(), false);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
            Assertions.assertEquals(0, os.getKeyLogs(config.getObjectFactory().getDefaultKey()).size());
        }
    }

    @Test
    public void testLogListenerChange() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintRuntimeLogs(true);
        config.setEnableLogCache(true);//enable log cache
        config.setLogListener(new LogListener1());
        String defaultKey = config.getObjectFactory().getDefaultKey();

        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            String logbackContent = logCollector.endLogCollector();
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
            Assertions.assertNotNull(logbackContent);
            Assertions.assertTrue(logbackContent.contains("LogListener1.onMethodStart"));
            Assertions.assertTrue(logbackContent.contains("LogListener1.onMethodEnd"));

            //change to new listener
            os.changeLogListener(defaultKey, new LogListener3());
            LogCollector logCollector1 = LogCollector.startLogCollector();
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                String logbackContent1 = logCollector1.endLogCollector();
                Assertions.assertTrue(logbackContent1.contains("LogListener3.onMethodStart"));
                Assertions.assertTrue(logbackContent1.contains("LogListener3.onMethodEnd"));
            }
        }
    }
}
