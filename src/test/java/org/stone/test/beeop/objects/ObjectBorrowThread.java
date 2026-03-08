/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects;

import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.objects.book.Book;

import java.util.concurrent.locks.LockSupport;

/**
 * Object borrow thread
 *
 * @author Chris Liao
 */
public class ObjectBorrowThread extends Thread {
    private final String objectKey;
    private final BeeObjectSource<String, Book> os;
    private long runTime;
    private BeeObjectHandle<String, Book> objectHale;
    private Exception failureCause;

    public ObjectBorrowThread(BeeObjectSource<String, Book> os) {
        this(os, null);
    }

    public ObjectBorrowThread(BeeObjectSource<String, Book> os, String objectKey) {
        this.os = os;
        this.objectKey = objectKey;
        this.setDaemon(true);
    }

    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    public Exception getFailureCause() {
        return failureCause;
    }

    public BeeObjectHandle<String, Book> getObjectHandle() {
        return objectHale;
    }

    public void run() {
        if (runTime > 0L) LockSupport.parkNanos(runTime - System.nanoTime());

        try {
            if (objectKey != null) {
                objectHale = os.getObjectHandle(objectKey);
            } else {
                objectHale = os.getObjectHandle();
            }
        } catch (Exception e) {
            this.failureCause = e;
        } finally {
            if (objectHale != null) TestUtil.oclose(objectHale);
        }
    }
}