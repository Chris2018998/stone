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

/**
 * Book Impl
 *
 * @author Chris Liao
 */
public class TextBook implements Book, BookBorrowInfo {
    private String title;
    private String author;

    private String borrower;
    private long borrowedTime;

    private long sleepTimeMs;
    private Exception failException;

    public TextBook() {
    }

    public TextBook(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public void setSleepTimeMs(long sleepTimeMs) {
        this.sleepTimeMs = sleepTimeMs;
    }

    public void setFailException(Exception failException) {
        this.failException = failException;
    }

    private void block() throws Exception {
        if (this.failException != null) throw failException;
        if (sleepTimeMs > 0) Thread.sleep(sleepTimeMs);
    }

    @Override
    public String getTitle() throws Exception {
        this.block();
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getAuthor() throws Exception {
        this.block();
        return author;
    }

    @Override
    public void setAuthor(String author) throws Exception {
        this.block();
        this.author = author;
    }

    @Override
    public String getBorrower() {
        return borrower;
    }

    public void setBorrower(String borrower) {
        this.borrower = borrower;
    }

    @Override
    public long getBorrowedTime() {
        return borrowedTime;
    }

    public void setBorrowedTime(long borrowedTime) {
        this.borrowedTime = borrowedTime;
    }
}