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
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory3;
import org.stone.test.beeop.objects.predicate.JavaBookPredicate;

/**
 * @author Chris Liao
 */
public class Tc0061ObjectEvictionTest {

    @Test
    public void testEvictionByMethodName() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.addObjectMethodName("setAuthor");
        config.setPredicate(new JavaBookPredicate());
        TextBookFactory3 bookFactory = new TextBookFactory3();
        bookFactory.setException(new Exception("eviction"));
        config.setObjectFactory(bookFactory);

        //getAuthor() not In Method Name list
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectHandle<String, Book> handle = null;
            try {
                handle = os.getObjectHandle();
                handle.call("getAuthor");
            } catch (Throwable e) {
                Assertions.assertNotNull(handle);
                Assertions.assertFalse(handle.isClosed());
                Assertions.assertNotEquals("Object handle has been closed", handle.toString());
            } finally {
                if (handle != null) handle.close();
            }

            try {
                handle = os.getObjectHandle();
                handle.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce Eckel2"});
            } catch (Throwable e) {
                Assertions.assertNotNull(handle);
                Assertions.assertTrue(handle.isClosed());
                Assertions.assertEquals("Object handle has been closed", handle.toString());
            } finally {
                if (handle != null) handle.close();
            }
        }
    }

    @Test
    public void testEvictionByAnyException() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setPredicate(new JavaBookPredicate());
        TextBookFactory3 bookFactory = new TextBookFactory3();
        bookFactory.setException(new Exception("eviction"));
        config.setObjectFactory(bookFactory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            BeeObjectHandle<String, Book> handle = null;
            try {
                handle = os.getObjectHandle();
                handle.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce Eckel2"});
            } catch (Throwable e) {
                Assertions.assertNotNull(handle);
                Assertions.assertTrue(handle.isClosed());
                Assertions.assertEquals("Object handle has been closed", handle.toString());
            } finally {
                if (handle != null) handle.close();
            }
        }
    }
}
