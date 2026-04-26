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

import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.book.TextBook;

/**
 * Book Factory
 *
 * @author Chris Liao
 */
public class AliveTestFactory extends TextBookFactory {
    private boolean alive = true;

    public AliveTestFactory(String title, String author) {
        super(title, author);
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public boolean isValid(String key, Book obj, int timeout) throws Exception {
        if (this.creationException != null) throw creationException;
        return alive;
    }

    @Override
    public void setDefault(String key, Book obj) throws Exception {
        TextBook book = (TextBook) obj;
        book.setTitle(title);
        book.setAuthor(author);
    }

    @Override
    public void reset(String key, Book obj) throws Exception {
        this.setDefault(key, obj);
    }
}