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
import org.stone.beeop.exception.BeePooledObjectGetTimeoutException;
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0082PooledObjectAliveTest {

    @Test
    public void testAliveAssumeTime() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setMaxWait(1L);
        config.setAliveAssumeTime(Long.MAX_VALUE);//---- TEST POINT
        config.setAliveTestTimeout(1);
        config.setParkTimeForRetry(1L);
        config.setUseThreadLocal(false);
        TextBookFactory bookFactory = new TextBookFactory("Java Concurrent", "DougLee");
        config.setObjectFactory(bookFactory);
        bookFactory.setBookIsValid(true);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object object1;
            try (BeeObjectHandle<String, Book> handle1 = os.getObjectHandle()) {
                object1 = TestUtil.getFieldValue(handle1, "instance");
            }

            Object object2;
            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                object2 = TestUtil.getFieldValue(handle2, "instance");
            }
            Assertions.assertEquals(object1, object2);
        }
    }

    @Test
    public void testAliveTestFailure() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setMaxWait(1L);
        config.setAliveAssumeTime(1L);
        config.setAliveTestTimeout(1);
        config.setParkTimeForRetry(1L);
        config.setUseThreadLocal(false);
        TextBookFactory bookFactory = new TextBookFactory("Java Concurrent", "DougLee");
        config.setObjectFactory(bookFactory);
        bookFactory.setBookIsValid(true);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object object1;
            try (BeeObjectHandle<String, Book> handle1 = os.getObjectHandle()) {
                object1 = TestUtil.getFieldValue(handle1, "instance");
            }

            Object object2;
            Thread.sleep(10L);
            bookFactory.setBookIsValid(false);
            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                object2 = TestUtil.getFieldValue(handle2, "instance");
            }
            Assertions.assertNotEquals(object1, object2);


            Object object3;
            Thread.sleep(10L);
            bookFactory.setBookIsValid(true);
            bookFactory.addFactoryMethodException("isValid", new Exception("alive test failed"));

            try (BeeObjectHandle<String, Book> handle3 = os.getObjectHandle()) {
                object3 = TestUtil.getFieldValue(handle3, "instance");
                Assertions.assertNotEquals(object2, object3);
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectGetTimeoutException.class, e);
            }
        }
    }
}
