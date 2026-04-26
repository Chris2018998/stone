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
import org.stone.beeop.BeeMethodLog;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeePooledObjectCreationException;
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.methodLog.LogListener1;
import org.stone.test.beeop.objects.methodLog.LogListener3;

import java.util.Objects;

/**
 * @author Chris Liao
 */
public class Tc0039ObjectSourcePoolLogsTest {

    @Test
    public void testSuccessLogOnNewKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setMaxKeySize(2);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintRuntimeLogs(true);
        config.setEnableLogCache(true);
        config.setObjectFactory(new TextBookFactory());
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.getPoolLogs().size());//default key

            String newKey = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey)) {
                Assertions.assertEquals(2, os.getPoolLogs().size());
                for (BeeMethodLog<String> log : os.getPoolLogs()) {
                    Assertions.assertTrue(log.isSuccessful());
                }

                os.clearPoolLogs();
                Assertions.assertEquals(0, os.getPoolLogs().size());
            }
        }
    }

    @Test
    public void testExceptionLogOnNewKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setMaxKeySize(2);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintRuntimeLogs(true);
        config.setEnableLogCache(true);

        TextBookFactory bookFactory = new TextBookFactory();
        config.setObjectFactory(bookFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.getPoolLogs().size());//default key

            String newKey = "Thanking in Rust";
            Exception failureException = new Exception("unknown error");
            bookFactory.setCreationException(failureException);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey)) {
                for (BeeMethodLog<String> log : os.getPoolLogs()) {
                    if (Objects.equals(newKey, log.getKey())) {
                        Assertions.assertFalse(log.isSuccessful());
                    }
                }
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectCreationException.class, e);
                Assertions.assertEquals(failureException, e.getCause());
            }
        }
    }

    @Test
    public void testLogCacheEnable() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setMaxKeySize(10);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setLogCacheSize(1);
        config.setEnableLogCache(true);//enable log cache

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //1: check log of default key
            Assertions.assertTrue(os.getPoolMonitorVo(false).isEnabledMethodLogCache());
            Assertions.assertEquals(1, os.getPoolLogs().size());//default key
            os.clearPoolLogs();
            Assertions.assertEquals(0, os.getPoolLogs().size());//default key

            //2: disable method log cache
            os.enableLogCache(false);
            Assertions.assertFalse(os.getPoolMonitorVo(false).isEnabledMethodLogCache());
            String newKey1 = "Thanking in C++";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey1)) {
                Assertions.assertTrue(os.existsKey(newKey1));
                Assertions.assertEquals(0, os.getPoolLogs().size());//no log generated
            }

            //3: re-enable log cache
            os.enableLogCache(true);
            os.enableLogCache(true);
            Assertions.assertTrue(os.getPoolMonitorVo(false).isEnabledMethodLogCache());
            String newKey2 = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey2)) {
                Assertions.assertTrue(os.existsKey(newKey2));
                Assertions.assertEquals(1, os.getPoolLogs().size());//one log generated
            }
        }
    }

    @Test
    public void testSmallLogCache() throws Exception {

        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setLogCacheSize(1);
        config.setEnableLogCache(true);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.getPoolLogs().size());
            Assertions.assertEquals(config.getObjectFactory().getDefaultKey(), os.getPoolLogs().get(0).getKey());

            String newKey1 = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey1)) {
                Assertions.assertTrue(os.existsKey(newKey1));
                Assertions.assertEquals(1, os.getPoolLogs().size());//no log generated
                Assertions.assertEquals(newKey1, os.getPoolLogs().get(0).getKey());

            }
            String newKey2 = "Thanking in C++";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey2)) {
                Assertions.assertTrue(os.existsKey(newKey2));
                Assertions.assertEquals(1, os.getPoolLogs().size());//no log generated
                Assertions.assertEquals(newKey2, os.getPoolLogs().get(0).getKey());

            }
        }

        config.setLogCacheSize(3);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            String newKey1 = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey1)) {
                Assertions.assertTrue(os.existsKey(newKey1));
                Assertions.assertEquals(2, os.getPoolLogs().size());//no log generated
            }

            String newKey2 = "Thanking in C++";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey2)) {
                Assertions.assertTrue(os.existsKey(newKey2));
                Assertions.assertEquals(3, os.getPoolLogs().size());//no log generated
            }
        }
    }

    @Test
    public void testLogListenerChange() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setMaxKeySize(10);
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setPrintRuntimeLogs(true);
        config.setEnableLogCache(true);//enable log cache
        config.setLogListener(new LogListener1());

        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            String logbackContent = logCollector.endLogCollector();
            Assertions.assertNotNull(logbackContent);
            Assertions.assertTrue(logbackContent.contains("LogListener1.onMethodStart"));
            Assertions.assertTrue(logbackContent.contains("LogListener1.onMethodEnd"));

            //change to new listener
            os.changeLogListener(new LogListener3());
            String newKey1 = "Thanking in C++";
            LogCollector logCollector1 = LogCollector.startLogCollector();
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey1)) {
                String logbackContent1 = logCollector1.endLogCollector();
                Assertions.assertTrue(logbackContent1.contains("LogListener3.onMethodStart"));
                Assertions.assertTrue(logbackContent1.contains("LogListener3.onMethodEnd"));
            }
        }
    }
}
