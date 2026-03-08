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
    public void testStartupSuccess() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        //1: with default pool impl
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }

        //2: with test pool impl
        config.setPoolImplementClassName(BookPool.class.getName());
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertNotNull(os);
        }
    }

    @Test
    public void testStartupFailure() {
        //1: invalid pool class
        BeeObjectSourceConfig<String, Book> config1 = OsConfigFactory.createDefault();
        config1.setPoolImplementClassName(BookPool.class.getName() + "_NotFound");
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config1)) {
            Assertions.fail("<testStartupFailure>test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreatedException.class, e);
        }

        //2: invalid object factory class
        BeeObjectSourceConfig<String, Book> config2 = new BeeObjectSourceConfig<>();
        config2.setObjectFactoryClassName(TextBookFactory.class.getName() + "_NotFound");
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config2)) {
            Assertions.fail("<testStartupFailure>test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreatedException.class, e);
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
    public void testCasOnInitialization() throws Exception {
        try (ObjectPool<String, Book> pool = new ObjectPool<>()) {
            BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
            CasThread thread1 = new CasThread(pool, config);
            CasThread thread2 = new CasThread(pool, config);
            long targetTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
            thread1.setTagetTime(targetTime);
            thread2.setTagetTime(targetTime);

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
        private long tagetTime;
        private Exception failCause;

        public CasThread(ObjectPool<String, Book> pool, BeeObjectSourceConfig<String, Book> config) {
            this.config = config;
            this.pool = pool;
        }

        public void run() {
            LockSupport.parkNanos(tagetTime - System.nanoTime());
            try {
                pool.start(config);
            } catch (Exception e) {
                this.failCause = e;
            }
        }

        public void setTagetTime(long tagetTime) {
            this.tagetTime = tagetTime;
        }

        public Exception getFailCause() {
            return failCause;
        }
    }
}
