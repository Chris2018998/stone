/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects.book;

import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * Book Impl
 *
 * @author Chris Liao
 */
public class TextBook extends BaseBook {

    public TextBook(TextBookFactory bookFactory) {
        super(bookFactory);
    }

    public TextBook(String title, String author, TextBookFactory bookFactory) {
        super(title, author, bookFactory);
    }
}
