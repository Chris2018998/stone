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
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.predicate.JavaBookPredicate;

/**
 * @author Chris Liao
 */
public class Tc0085PooledObjectEvictionTest {

    @Test
    public void testNotSetPredicate() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        TextBookFactory bookFactory = new TextBookFactory();

        Exception evictException = new Exception("eviction");
        bookFactory.addObjectMethodException("setAuthor", evictException);
        bookFactory.addObjectMethodException("getAuthor", evictException);
        config.setObjectFactory(bookFactory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object object1 = null, object2 = null;

            //1: first call
            try (BeeObjectHandle<String, Book> handle1 = os.getObjectHandle()) {
                object1 = TestUtil.getFieldValue(handle1, "instance");
                handle1.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce2"});
                Assertions.fail("[Tc0085PooledObjectEvictionTest.testNoPredicate]failed");
            } catch (Throwable e) {
                Assertions.assertEquals("eviction", e.getMessage());
            }

            //2: second call
            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                object2 = TestUtil.getFieldValue(handle2, "instance");
                Assertions.assertEquals(object1, object2);
                handle2.call("getAuthor");
                Assertions.fail("[Tc0085PooledObjectEvictionTest.testNoPredicate]failed");
            } catch (Throwable e) {
                Assertions.assertEquals("eviction", e.getMessage());
            }

            //check existence
            try (BeeObjectHandle<String, Book> handle3 = os.getObjectHandle()) {
                Object object3 = TestUtil.getFieldValue(handle3, "instance");
                Assertions.assertEquals(object2, object3);
            }
        }
    }

    @Test
    public void testSetPredicate() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        TextBookFactory bookFactory = new TextBookFactory();
        Exception evictException = new Exception("eviction");
        bookFactory.addObjectMethodException("setAuthor", evictException);
        config.setObjectFactory(bookFactory);
        config.setPredicate(new JavaBookPredicate());

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object object1 = null, object2;
            try (BeeObjectHandle<String, Book> handle1 = os.getObjectHandle()) {
                object1 = TestUtil.getFieldValue(handle1, "instance");
                handle1.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce2"});
                Assertions.fail("[Tc0085PooledObjectEvictionTest.testNoPredicate]failed");
            } catch (Throwable e) {
                Assertions.assertEquals("eviction", e.getMessage());
            }

            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                object2 = TestUtil.getFieldValue(handle2, "instance");
                Assertions.assertNotEquals(object1, object2);
            }
        }
    }

    @Test
    public void testEvictByMethodName() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        TextBookFactory bookFactory = new TextBookFactory();
        Exception evictException = new Exception("eviction");
        bookFactory.addObjectMethodException("setAuthor", evictException);
        bookFactory.addObjectMethodException("getAuthor", evictException);
        config.setObjectFactory(bookFactory);
        config.setPredicate(new JavaBookPredicate());
        config.addObjectMethodName("setAuthor");//<---evict by method name

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object object1 = null;
            //1: evicted test
            try (BeeObjectHandle<String, Book> handle1 = os.getObjectHandle()) {
                object1 = TestUtil.getFieldValue(handle1, "instance");
                handle1.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce2"});
                Assertions.fail("[Tc0085PooledObjectEvictionTest.testNoPredicate]failed");
            } catch (Throwable e) {
                Assertions.assertEquals("eviction", e.getMessage());
            }

            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                Object object2 = TestUtil.getFieldValue(handle2, "instance");
                Assertions.assertNotEquals(object1, object2);
            }

            //2: not evicted test
            Object object3 = null;
            try (BeeObjectHandle<String, Book> handle3 = os.getObjectHandle()) {
                object3 = TestUtil.getFieldValue(handle3, "instance");
                handle3.call("getAuthor");
                Assertions.fail("[Tc0085PooledObjectEvictionTest.testNoPredicate]failed");
            } catch (Throwable e) {
                Assertions.assertEquals("eviction", e.getMessage());
            }
            try (BeeObjectHandle<String, Book> handle4 = os.getObjectHandle()) {
                Object object4 = TestUtil.getFieldValue(handle4, "instance");
                Assertions.assertEquals(object3, object4);
            }
        }


        //not evicted test
        Exception evictException2 = new Exception("eviction2");
        bookFactory.addObjectMethodException("setAuthor", evictException2);
        bookFactory.addObjectMethodException("setAuthor", evictException2);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object object1 = null;
            try (BeeObjectHandle<String, Book> handle1 = os.getObjectHandle()) {
                object1 = TestUtil.getFieldValue(handle1, "instance");
                handle1.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce2"});
                Assertions.fail("[Tc0085PooledObjectEvictionTest.testNoPredicate]failed");
            } catch (Throwable e) {
                Assertions.assertEquals("eviction2", e.getMessage());
            }
            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                Object object2 = TestUtil.getFieldValue(handle2, "instance");
                Assertions.assertEquals(object1, object2);
            }
        }
    }
}
