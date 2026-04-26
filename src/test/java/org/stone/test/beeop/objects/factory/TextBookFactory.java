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

import java.util.concurrent.locks.LockSupport;

/**
 * Book Factory
 *
 * @author Chris Liao
 */
public class TextBookFactory implements BeeObjectFactory<String, Book> {
    protected String blockType;
    protected long blockTime;

    protected String defaultKey;
    protected String title;
    protected String author;
    protected int stockCount;
    protected Exception creationException;


    protected long callSleepTimeMs;
    protected Exception callException;

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

    public void setCreationException(Exception creationException) {
        this.creationException = creationException;
    }

    public void setCallException(Exception callException) {
        this.callException = callException;
    }

    public void setCallSleepTimeMs(long callSleepTimeMs) {
        this.callSleepTimeMs = callSleepTimeMs;
    }

    @Override
    public String getDefaultKey() {
        return defaultKey;
    }

    public void setDefaultKey(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    @Override
    public boolean isValid(String key, Book obj, int timeout) throws Exception {
        return true;
    }

    @Override
    public void setDefault(String key, Book obj) throws Exception {
        TextBook book = (TextBook) obj;
        book.setBorrower(null);
        book.setBorrowedTime(0L);
    }

    @Override
    public void reset(String key, Book obj) throws Exception {
        this.setDefault(key, obj);
    }

    @Override
    public void destroy(String key, Book obj) throws Exception {
        TextBook book = (TextBook) obj;
    }

    @Override
    public Book create(String key) throws Exception {
        if (BlockWayTypes.Type_Sleep.equals(blockType)) {
            if (blockTime > 0L) Thread.sleep(blockTime);
        } else if (BlockWayTypes.Type_park.equals(blockType)) {
            if (blockTime > 0L) {
                LockSupport.parkNanos(blockTime);
            } else {
                LockSupport.park();
            }
        }

        if (this.creationException != null) throw this.creationException;
        TextBook book = new TextBook(this.title, this.author);
        if (callException != null) book.setFailException(callException);
        if (callSleepTimeMs > 0) book.setSleepTimeMs(callSleepTimeMs);
        return book;
    }

    public void setBlock(String blockType, long blockTime) {
        this.blockType = blockType;
        this.blockTime = blockTime;
    }
}
