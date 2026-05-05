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

import org.stone.beeop.*;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Book Pool
 *
 * @author Chris Liao
 */
public class BookPool implements BeeObjectPool<String, Book> {
    private final TextBookFactory bookFactory;
    private boolean closed;
    private boolean blockFlag;
    private boolean usePark;
    private long blockTime;

    public BookPool() {
        this.bookFactory = new TextBookFactory();
    }

    public BeeObjectHandle<String, Book> getObjectHandle() throws Exception {
        block();
        String bookKey = bookFactory.getDefaultKey();
        return new BookHandle(bookKey, bookFactory.create(bookKey));
    }

    public BeeObjectHandle<String, Book> getObjectHandle(String key) throws Exception {
        block();
        String bookKey = bookFactory.getDefaultKey();
        return new BookHandle(bookKey, bookFactory.create(bookKey));
    }

    public void setBlockFlag(boolean blockFlag) {
        this.blockFlag = blockFlag;
    }

    public void setUsePark(boolean usePark) {
        this.usePark = usePark;
    }

    public void setBlockTime(long blockTime) {
        this.blockTime = blockTime;
    }

    private void block() throws Exception {
        if (blockFlag) {
            if (usePark) {
                if (blockTime > 0L) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(blockTime));
                } else {
                    LockSupport.park();
                }
            } else {
                if (blockTime > 0L) {
                    Thread.sleep(blockTime);
                } else {
                    Thread.sleep(Long.MAX_VALUE);
                }
            }
        }
    }

    public int bucketSize() throws Exception {
        return 10;
    }

    public boolean existsBucket(String key) throws Exception {
        return true;
    }

    public boolean suspendBucket(String key) throws Exception {
        return true;
    }

    public boolean resumeBucket(String key) throws Exception {
        return true;
    }

    public void clearBucketObjects(String key) throws Exception {

    }

    public void clearBucketObjects(String key, boolean forceRecycleBorrowed) throws Exception {

    }

    public boolean deleteBucket(String key) throws Exception {
        return true;
    }

    public boolean deleteBucket(String key, boolean forceRecycleBorrowed) throws Exception {
        return true;
    }

    public void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public boolean suspend() throws Exception {
        return true;
    }

    public boolean resume() throws Exception {
        return true;
    }

    public void start(BeeObjectSourceConfig<String, Book> config) throws Exception {

    }

    public void restart(boolean forceRecycleBorrowed) throws Exception {

    }

    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<String, Book> config) throws Exception {

    }

    public void enableLogPrinter(boolean enable) throws Exception {

    }

    public void enableBucketLogPrinter(String key, boolean enable) throws Exception {

    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo(boolean includekeys) throws Exception {
        return null;
    }

    public BeeObjectBucketMonitorVo getBucketMonitorVo(String key) throws Exception {
        return null;
    }

    public List<Thread> interruptWaitingThreadsInBuckets() throws Exception {
        return null;
    }


    public List<Thread> interruptWaitingThreadsInBucket(String key) throws Exception {
        return null;
    }

    public void enablePoolLogCache(boolean enable) throws Exception {

    }

    public void changePoolLogListener(BeeMethodLogListener<String> listener) throws Exception {

    }

    public void clearPoolLogs() throws Exception {

    }

    public List<BeeMethodLog<String>> getPoolLogs() throws Exception {
        return null;
    }

    public void enableBucketLogCache(String key, boolean enable) throws Exception {

    }

    public void changeBucketLogListener(String key, BeeMethodLogListener<String> listener) throws Exception {

    }

    public void clearBucketLogs(String key) throws Exception {

    }

    public List<BeeMethodLog<String>> getBucketLogs(String key) throws Exception {
        return null;
    }

    public void clearBucketObjectLogs(String key) throws Exception {

    }

    public List<BeeMethodLog<String>> getBucketObjectLogs(String key) throws Exception {
        return null;
    }
}
