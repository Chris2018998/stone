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

    BookPool() {
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

    public int keySize() throws Exception {
        return 10;
    }

    public boolean existsKey(String key) throws Exception {
        return true;
    }

    public boolean suspendKey(String key) throws Exception {
        return true;
    }

    public boolean resumeKey(String key) throws Exception {
        return true;
    }

    public void clearKeyObjects(String key) throws Exception {

    }

    public void clearKeyObjects(String key, boolean forceRecycleBorrowed) throws Exception {

    }

    public boolean deleteKey(String key) throws Exception {
        return true;
    }

    public boolean deleteKey(String key, boolean forceRecycleBorrowed) throws Exception {
        return true;
    }

    public void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public boolean suspendPool() throws Exception {
        return true;
    }

    public boolean resumePool() throws Exception {
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

    public void enableLogPrinter(String key, boolean enable) throws Exception {

    }

    public BeeObjectPoolMonitorVo<String> getPoolMonitorVo(boolean includekeys) throws Exception {
        return null;
    }

    public BeeObjectKeyMonitorVo<String> getKeyMonitorVo(String key) throws Exception {
        return null;
    }

    public List<Thread> interruptWaitingThreads() throws Exception {
        return null;
    }


    public List<Thread> interruptWaitingThreads(String key) throws Exception {
        return null;
    }

    public void enableLogCache(boolean enable) throws Exception {

    }

    public void changeLogListener(BeeMethodLogListener<String> listener) throws Exception {

    }

    public void clearPoolLogs() throws Exception {

    }

    public List<BeeMethodLog<String>> getPoolLogs() throws Exception {
        return null;
    }

    public void enableLogCache(String key, boolean enable) throws Exception {

    }

    public void changeLogListener(String key, BeeMethodLogListener<String> listener) throws Exception {

    }

    public void clearKeyLogs(String key) throws Exception {

    }

    public List<BeeMethodLog<String>> getKeyLogs(String key) throws Exception {
        return null;
    }

    public void clearKeyObjectLogs(String key) throws Exception {

    }

    public List<BeeMethodLog<String>> getKeyObjectLogs(String key) throws Exception {
        return null;
    }
}
