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
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0080PooledObjectCreationTest {

    @Test
    public void testExceptionFromFactory() {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>()) {
            TextBookFactory factory = new TextBookFactory("Thanking in Java", "Bruce Eckel");
            factory.addFactoryMethodException("setDefault", new Exception("unknown exception"));
            os.setObjectFactory(factory);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0080PooledObjectCreationTest.testExceptionFromFactory]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectCreationException.class, e);
                Assertions.assertEquals("unknown exception", e.getCause().getMessage());
            }
        }
    }

    @Test
    public void testNullObjectFromFactory() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        TextBookFactory factory = new TextBookFactory("Thanking in Java", "Bruce Eckel");
        factory.setNullResultFlag(true);
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
