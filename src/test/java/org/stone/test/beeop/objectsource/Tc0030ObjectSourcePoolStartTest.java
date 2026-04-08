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
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.beeop.exception.BeeObjectSourceCreationException;
import org.stone.beeop.exception.BeeObjectSourcePoolStartedFailureException;
import org.stone.beeop.exception.BeePooledObjectCreationException;
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
    public void testPoolStartInOsConstructor() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(os.getPoolMonitorVo(false).isReady());

            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            } catch (Throwable e) {
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testPoolStartInOsConstructor]failed");
            }
        }
    }

    @Test
    public void testPoolLazyStartByCallingGetObjectHandle() throws Exception {
        //1: getObjectHandle()
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            Assertions.assertTrue(os.getPoolMonitorVo(false).isLazy());
            Assertions.assertFalse(os.getPoolMonitorVo(false).isReady());
            os.setObjectFactory(new TextBookFactory());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertFalse(os.getPoolMonitorVo(false).isLazy());
                Assertions.assertTrue(os.getPoolMonitorVo(false).isReady());
            }
        }

        //2:getObjectHandle(default key)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            Assertions.assertTrue(os.getPoolMonitorVo(false).isLazy());
            Assertions.assertFalse(os.getPoolMonitorVo(false).isReady());

            TextBookFactory bookFactory = new TextBookFactory();
            os.setObjectFactory(bookFactory);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(bookFactory.getDefaultKey())) {
                Assertions.assertFalse(os.getPoolMonitorVo(false).isLazy());
                Assertions.assertTrue(os.getPoolMonitorVo(false).isReady());
            }
        }


        //3:getObjectHandle(new key)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            Assertions.assertTrue(os.getPoolMonitorVo(false).isLazy());
            Assertions.assertFalse(os.getPoolMonitorVo(false).isReady());

            TextBookFactory bookFactory = new TextBookFactory();
            os.setObjectFactory(bookFactory);

            String newKey = "Thanking in Rust";
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle(newKey)) {
                Assertions.assertFalse(os.getPoolMonitorVo(false).isLazy());
                Assertions.assertTrue(os.getPoolMonitorVo(false).isReady());
                Assertions.assertTrue(os.existsKey(bookFactory.getDefaultKey()));
                Assertions.assertTrue(os.existsKey(newKey));
            }
        }
    }


    @Test
    public void testNullConfiguration() {
        try (BeeObjectSource<String, Book> ignored1 = new BeeObjectSource<>(null)) {
            Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testNullConfiguration]test failed");
        } catch (RuntimeException e) {
            Assertions.assertInstanceOf(NullPointerException.class, e);
        }
    }

    @Test
    public void testPoolClassNotFound() {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setPoolImplementClassName(BookPool.class.getName() + "_NotFound");

        //1: throw exception from Object source constructor
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testPoolClassNotFound]test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreationException.class, e);
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }

        //2: throw exception from Object source method call(get)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactory(new TextBookFactory());
            os.setPoolImplementClassName(BookPool.class.getName() + "_NotFound");

            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testPoolClassNotFound]test failed");
            } catch (Throwable e) {
                Assertions.assertInstanceOf(ClassNotFoundException.class, e);
            }
        }
    }

    @Test
    public void testIncorrectConfiguration() {
        //1: failure check
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(10);
        config.setMaxActive(5);
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testIncorrectConfiguration]test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreationException.class, e);
            BeeObjectSourceCreationException e1 = (BeeObjectSourceCreationException) e;
            Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e1.getCause());
            BeeObjectSourcePoolStartedFailureException e2 = (BeeObjectSourcePoolStartedFailureException) e1.getCause();
            Assertions.assertInstanceOf(BeeObjectSourceConfigException.class, e2.getCause());
        }

        //2: failure check
        config = new BeeObjectSourceConfig<>();
        config.setObjectFactoryClassName(TextBookFactory.class.getName() + "_NotFound");
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testIncorrectConfiguration]test failed");
        } catch (Throwable e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreationException.class, e);
            BeeObjectSourceCreationException e1 = (BeeObjectSourceCreationException) e;
            Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e1.getCause());
            BeeObjectSourcePoolStartedFailureException e2 = (BeeObjectSourcePoolStartedFailureException) e1.getCause();
            Assertions.assertInstanceOf(BeeObjectSourceConfigException.class, e2.getCause());

            BeeObjectSourceConfigException e3 = (BeeObjectSourceConfigException) e2.getCause();
            Assertions.assertInstanceOf(ClassNotFoundException.class, e3.getCause());
        }

        //3: failure check(lazy)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setInitialSize(10);
            os.setMaxActive(5);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testIncorrectConfiguration]test failed");
            } catch (Throwable e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e);
                BeeObjectSourcePoolStartedFailureException e1 = (BeeObjectSourcePoolStartedFailureException) e;
                Assertions.assertInstanceOf(BeeObjectSourceConfigException.class, e1.getCause());
            }
        }

        //4: failure check(lazy)
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            os.setObjectFactoryClassName(TextBookFactory.class.getName() + "_NotFound");
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testIncorrectConfiguration]test failed");
            } catch (Throwable e) {
                Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e);
                BeeObjectSourcePoolStartedFailureException e1 = (BeeObjectSourcePoolStartedFailureException) e;
                Assertions.assertInstanceOf(BeeObjectSourceConfigException.class, e1.getCause());
                BeeObjectSourceConfigException e2 = (BeeObjectSourceConfigException) e1.getCause();
                Assertions.assertInstanceOf(ClassNotFoundException.class, e2.getCause());
            }
        }
    }

    @Test
    public void testInitializedFailureOnDefaultKey() {//Initialized failure on default key during key pool startup
        TextBookFactory factory = new TextBookFactory();
        factory.setException(new Exception("Paper is not enough"));
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setObjectFactory(factory);
        config.setInitialSize(1);
        config.setMaxActive(1);
        try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
            Assertions.fail("[Tc0030ObjectSourcePoolStartTest.testInitializedFailureOnDefaultKey]failed");
        } catch (Exception e) {
            Assertions.assertInstanceOf(BeeObjectSourceCreationException.class, e);
            BeeObjectSourceCreationException e1 = (BeeObjectSourceCreationException) e;
            Assertions.assertInstanceOf(BeeObjectSourcePoolStartedFailureException.class, e1.getCause());
            BeeObjectSourcePoolStartedFailureException e2 = (BeeObjectSourcePoolStartedFailureException) e1.getCause();
            Assertions.assertInstanceOf(BeePooledObjectCreationException.class, e2.getCause());
            BeePooledObjectCreationException e3 = (BeePooledObjectCreationException) e2.getCause();
            Assertions.assertEquals("Paper is not enough", e3.getCause().getMessage());
        }
    }

    //************************************************* test on Pool  ************************************************//
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
