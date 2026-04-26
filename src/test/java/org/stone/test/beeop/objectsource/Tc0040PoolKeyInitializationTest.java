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
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceCreationException;
import org.stone.beeop.exception.BeeObjectSourcePoolStartedFailureException;
import org.stone.beeop.exception.BeePooledObjectCreationException;
import org.stone.beeop.exception.BeePooledObjectGetInterruptedException;
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.factory.TextBookFactory2;

/**
 * @author Chris Liao
 */
public class Tc0040PoolKeyInitializationTest {

    @Test
    public void testWorkMode() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setFairMode(true);
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testWorkMode]test failed");
        }

        config.setFairMode(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testWorkMode]test failed");
        }
    }

    @Test
    public void testThreadLocal() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setUseThreadLocal(true);
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testWorkMode]test failed");
        }

        config.setUseThreadLocal(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testWorkMode]test failed");
        }


        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createDefault();
        config.setUseThreadLocal(true);
        config.setInitialSize(0);
        config.setMaxActive(1);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config2)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testWorkMode]test failed");
        }

        config.setUseThreadLocal(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config2)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertNotNull(handle);
            }
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testWorkMode]test failed");
        }
    }

    @Test
    public void testAsyncInitialization() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setAsyncCreateInitObjects(false);
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testAsyncInitialization]test failed");
        }

        config.setAsyncCreateInitObjects(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testAsyncInitialization]test failed");
        }

        TextBookFactory bookFactory = new TextBookFactory();
        bookFactory.setCreationException(new Exception("Failed to create book"));
        config.setObjectFactory(bookFactory);
        config.setPrintRuntimeLogs(true);
        LogCollector logCollector = LogCollector.startLogCollector();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        } catch (Throwable e) {
            Assertions.fail("[Tc0040PoolKeyInitializationTest.testAsyncInitialization]test failed");
        }
        String logContent = logCollector.endLogCollector();
        Assertions.assertTrue(logContent.contains("Failed to create initial objects during async mode"));
    }

    @Test
    public void testNullObjectFromFactory() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        TextBookFactory2 factory = new TextBookFactory2("Thanking in Java", "Bruce Eckel");
        config.setObjectFactory(factory);
        PoolStatThread startupThread1 = new PoolStatThread(config);
        startupThread1.start();
        startupThread1.join();
        Assertions.assertInstanceOf(BeeObjectSourceCreationException.class, startupThread1.failureException);
        Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, startupThread1.failureException.getCause());
        Assertions.assertInstanceOf(BeePooledObjectCreationException.class, startupThread1.failureException.getCause().getCause());

        PoolStatThread startupThread2 = new PoolStatThread(config);
        startupThread2.interrupt();
        startupThread2.start();
        startupThread2.join();
        Assertions.assertInstanceOf(BeeObjectSourceCreationException.class, startupThread2.failureException);
        Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, startupThread2.failureException.getCause());
        Assertions.assertInstanceOf(BeePooledObjectGetInterruptedException.class, startupThread2.failureException.getCause().getCause());
    }

    private static class PoolStatThread extends Thread {
        private final BeeObjectSourceConfig<String, Book> config;
        private Throwable failureException;

        public PoolStatThread(BeeObjectSourceConfig<String, Book> config) {
            this.config = config;
        }

        public void run() {
            try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            } catch (Throwable e) {
                this.failureException = e;
            }
        }
    }
}
