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
import org.stone.beeop.exception.BeePooledObjectCreationException;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.AliveTestFactory;

/**
 * @author Chris Liao
 */
public class Tc0058PooledObjectAliveTest {

    @Test
    public void testAlive() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setMaxWait(1L);
        config.setAliveAssumeTime(1L);
        config.setAliveTestTimeout(1);
        config.setParkTimeForRetry(1L);
        AliveTestFactory bookFactory = new AliveTestFactory("Java Concurrent", "DougLee");
        config.setObjectFactory(bookFactory);
        bookFactory.setAlive(true);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Thread.sleep(10L);
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            } catch (Exception e) {
                Assertions.fail("[Tc0058PooledObjectAliveTest.testAlive]");
            }

            Thread.sleep(10L);
            bookFactory.setAlive(false);
            bookFactory.setCreationException(new Exception("alive test failed"));
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0058PooledObjectAliveTest.testAlive]");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectCreationException.class, e);
            }
        }
    }
}
