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

/**
 * Book Factory
 *
 * @author Chris Liao
 */
public class TextBookFactory2 extends TextBookFactory {

    public TextBookFactory2(String title, String author) {
        super(title, author);
    }

    @Override
    public Book create(String key) throws Exception {
        return null;
    }
}
