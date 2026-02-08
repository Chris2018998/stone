/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects.factory;

import org.stone.beeop.BeeObjectFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.book.TextBook;

/**
 * Book Factory
 *
 * @author Chris Liao
 */
public class TextBookFactory implements BeeObjectFactory<String, Book> {
    protected String defaultKey;
    protected String title;
    protected String author;
    protected int stockCount;

    public TextBookFactory() {
        this("Thanking in Java", "Bruce Eckel");
    }

    public TextBookFactory(String title, String author) {
        this.title = title;
        this.author = author;
        this.defaultKey = title;
    }

    public int getStockCount() {
        return stockCount;
    }

    public void setStockCount(int stockCount) {
        this.stockCount = stockCount;
    }

    @Override
    public String getDefaultKey() {
        return defaultKey;
    }

    public void setDefaultKey(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    @Override
    public boolean isValid(String key, Book obj, int timeout) {
        return true;
    }

    @Override
    public void setDefault(String key, Book obj) {
        TextBook book = (TextBook) obj;
        book.setBorrower(null);
        book.setBorrowedTime(0L);
    }

    @Override
    public void reset(String key, Book obj) {
        this.setDefault(key, obj);
    }

    @Override
    public void destroy(String key, Book obj) {
        TextBook book = (TextBook) obj;
    }

    @Override
    public Book create(String key) throws Exception {
        return new TextBook(this.title, this.author);
    }
}
