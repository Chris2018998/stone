/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects.pool;

import org.stone.beeop.BeeObjectHandle;
import org.stone.test.beeop.objects.book.Book;

/**
 * Book handle
 *
 * @author Chris Liao
 */

public class BookHandle implements BeeObjectHandle<String, Book> {
    private final String key;
    private final Book book;

    public BookHandle(String key, Book book) {
        this.key = key;
        this.book = book;
    }

    public String getKey() {
        return key;
    }

    public Book getObject() {
        return book;
    }

    public long getLastAccessedTime() {
        return 0L;
    }

    public Object call(String methodName) {
        return null;
    }

    public Object call(String methodName, Class<?>[] paramTypes, Object[] paramValues) {
        return null;
    }

    public boolean isClosed() {
        return false;
    }

    public void close() {
    }

    public void abort() {
    }
}
