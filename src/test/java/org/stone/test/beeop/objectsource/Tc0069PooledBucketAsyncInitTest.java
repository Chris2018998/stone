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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0069PooledBucketAsyncInitTest {

    @Test
    public void testAsyncInitialization() {
        //1: async mode(initialization size==0)
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setAsyncCreateInitObjects(true);
        config.setInitialSize(0);
        config.setMaxActive(1);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        } catch (Throwable e) {
            Assertions.fail("[Tc0069PooledBucketAsyncInitTest.testAsyncInitialization]test failed");
        }

        //2: async mode(initialization size>0)
        config.setInitialSize(1);
        config.setMaxActive(1);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        } catch (Throwable e) {
            Assertions.fail("[Tc0069PooledBucketAsyncInitTest.testAsyncInitialization]test failed");
        }

        //3: exception in async mode
        TextBookFactory bookFactory = new TextBookFactory();
        bookFactory.addFactoryMethodException("create", new Exception("Failed to create book"));
        config.setObjectFactory(bookFactory);
        config.setPrintRuntimeLogs(true);
        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Thread.sleep(100L);
        } catch (Throwable e) {
            Assertions.fail("[Tc0069PooledBucketAsyncInitTest.testAsyncInitialization]test failed");
        }
        String logContent = logCollector.endLogCollector();
        Assertions.assertTrue(logContent.contains("Failed to create initial objects during async mode"));
    }
}
