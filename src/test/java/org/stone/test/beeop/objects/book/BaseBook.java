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
public class BaseBook implements Book, BookBorrowInfo {
    private final TextBookFactory bookFactory;
    private String title;
    private String author;
    private String borrower;
    private long borrowedTime;

    public BaseBook(TextBookFactory bookFactory) {
        this.bookFactory = bookFactory;
    }

    public BaseBook(String title, String author, TextBookFactory bookFactory) {
        this.title = title;
        this.author = author;
        this.bookFactory = bookFactory;
    }

    @Override
    public String getTitle() throws Exception {
        bookFactory.beforeObjectMethodCall("getTitle");
        return title;
    }

    @Override
    public void setTitle(String title) throws Exception {
        bookFactory.beforeObjectMethodCall("setTitle");
        this.title = title;
    }

    @Override
    public String getAuthor() throws Exception {
        bookFactory.beforeObjectMethodCall("getAuthor");
        return author;
    }

    @Override
    public void setAuthor(String author) throws Exception {
        bookFactory.beforeObjectMethodCall("setAuthor");
        this.author = author;
    }

    @Override
    public String getBorrower() throws Exception {
        bookFactory.beforeObjectMethodCall("getBorrower");
        return borrower;
    }

    @Override
    public void setBorrower(String borrower) throws Exception {
        bookFactory.beforeObjectMethodCall("setBorrower");
        this.borrower = borrower;
    }

    @Override
    public long getBorrowedTime() throws Exception {
        bookFactory.beforeObjectMethodCall("getBorrowedTime");
        return borrowedTime;
    }

    @Override
    public void setBorrowedTime(long borrowedTime) throws Exception {
        bookFactory.beforeObjectMethodCall("setBorrowedTime");
        this.borrowedTime = borrowedTime;
    }

    @Override
    public void setDefault(String title, String author) throws Exception {
        this.title = title;
        this.author = author;
        this.borrower = null;
        this.borrowedTime = 0L;
    }
}
