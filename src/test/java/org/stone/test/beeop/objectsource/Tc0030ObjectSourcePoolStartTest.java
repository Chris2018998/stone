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
import org.stone.beeop.BeeObjectFactory;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceCreatedException;
import org.stone.beeop.exception.BeeObjectSourcePoolStartedFailureException;
import org.stone.beeop.pool.ObjectPool;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.pool.BookPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0030ObjectSourcePoolStartTest {

    @Test
    public void testLazyStartup() {
        TextBookFactory factory = new TextBookFactory();
        String defaultKey = factory.getDefaultKey();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());


        }
    }

    @Test
    public void testInvalidPoolClass() {
        //1: null configuration
        try (BeeObjectSource<String, Book> ignored1 = new BeeObjectSource<>(null)) {
            Assertions.fail("[testNullConfig]Test failed");
        } catch (RuntimeException e) {
            Assertions.assertInstanceOf(NullPointerException.class, e);
        }

        //2: pool startup in object source constructor
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setPoolImplementClassName(BookPool.class.getName() + "_NotFound");
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.fail("<testStartupFailure>test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreatedException.class, e);
        }

        //3: pool lazy startup
        BeeObjectFactory<String, Book> objectFactory = config.getObjectFactory();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setPoolImplementClassName(config.getPoolImplementClassName());
            os.setObjectFactory(config.getObjectFactory());
            try {
                os.getObjectHandle();
            } catch (Throwable e) {
                Assertions.assertInstanceOf(ClassNotFoundException.class, e);//Pool class not found
            }

            try {
                os.getObjectHandle(objectFactory.getDefaultKey());
            } catch (Throwable e) {
                Assertions.assertInstanceOf(ClassNotFoundException.class, e);//Pool class not found
            }
        }
    }

    @Test
    public void testCheckFailed() {
        //1: constructor
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createDefault();
        config1.setInitialSize(10);
        config1.setMaxActive(5);
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config1)) {
            Assertions.fail("<testCheckFailed>test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreatedException.class, e);
            BeeObjectSourceCreatedException ee = (BeeObjectSourceCreatedException) e;
            Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, ee.getCause());
        }

        BeeObjectSourceConfig<String, Book> config2 = new BeeObjectSourceConfig<>();
        config2.setObjectFactoryClassName(TextBookFactory.class.getName() + "_NotFound");
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config2)) {
            Assertions.fail("<testStartupFailure>test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreatedException.class, e);
            BeeObjectSourceCreatedException ee = (BeeObjectSourceCreatedException) e;
            Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, ee.getCause());
        }

        //2: lazy creation
        BeeObjectFactory<String, Book> objectFactory = config1.getObjectFactory();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(objectFactory);
            os.setInitialSize(10);
            os.setMaxActive(5);
            try {
                os.getObjectHandle();
            } catch (Throwable e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e);
            }

            try {
                os.getObjectHandle(objectFactory.getDefaultKey());
            } catch (Throwable e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e);//Pool class not found
            }
        }
    }


    @Test
    public void testNullConfig() {
        try (ObjectPool<String, Book> pool = new ObjectPool<>()) {
            pool.start(null);
            Assertions.fail("[testNullConfig]Test failed");
        } catch (Exception e) {
            Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e);
            Assertions.assertEquals("Object source configuration can't be null", e.getMessage());
        }
    }

    @Test
    public void testCasFailure() throws Exception {
        try (ObjectPool<String, Book> pool = new ObjectPool<>()) {
            BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
            long targetTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            CasThread thread1 = new CasThread(pool, config, targetTime);
            CasThread thread2 = new CasThread(pool, config, targetTime);
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            if (thread1.getFailCause() != null) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, thread1.getFailCause());
                Assertions.assertEquals("Object source pool is starting up or has already started", thread1.getFailCause().getMessage());
            } else if (thread2.getFailCause() != null) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, thread2.getFailCause());
                Assertions.assertEquals("Object source pool is starting up or has already started", thread2.getFailCause().getMessage());
            }
        }
    }

    private static class CasThread extends Thread {
        private final BeeObjectSourceConfig<String, Book> config;
        private final ObjectPool<String, Book> pool;
        private final long tagetTime;
        private Exception failCause;

        public CasThread(ObjectPool<String, Book> pool, BeeObjectSourceConfig<String, Book> config, long tagetTime) {
            this.config = config;
            this.pool = pool;
            this.tagetTime = tagetTime;
        }

        public void run() {
            LockSupport.parkNanos(tagetTime - System.nanoTime());
            try {
                pool.start(config);
            } catch (Exception e) {
                this.failCause = e;
            }
        }

        public Exception getFailCause() {
            return failCause;
        }
    }
}
