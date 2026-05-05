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

import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.objects.book.Book;

import java.util.concurrent.locks.LockSupport;

/**
 * Object borrow thread
 *
 * @author Chris Liao
 */
public class ObjectBorrowThread extends BaseThread {

    public ObjectBorrowThread(BeeObjectSource<String, Book> os) {
        this.objectSource = os;
    }

    public ObjectBorrowThread(BeeObjectSource<String, Book> os, String objectKey) {
        this.objectSource = os;
        this.objectKey = objectKey;
        this.setDaemon(true);
    }

    public ObjectBorrowThread(BeeObjectSourceConfig<String, Book> config) {
        this.config = config;
        this.setDaemon(true);
    }

    public ObjectBorrowThread(BeeObjectSourceConfig<String, Book> config, String objectKey) {
        this.config = config;
        this.objectKey = objectKey;
        this.setDaemon(true);
    }

    public void run() {
        if (this.targetTimeToRun > 0L) LockSupport.parkNanos(targetTimeToRun - System.nanoTime());

        try {
            if (objectSource == null) {
                if (this.config == null) throw new Exception("Configuration not set");
                this.objectSource = new BeeObjectSource<>(this.config);
            }

            if (objectKey != null) {
                objectHale = objectSource.getObjectHandle(objectKey);
            } else {
                objectHale = objectSource.getObjectHandle();
            }
        } catch (Exception e) {
            this.failureCause = e;
        } finally {
            if (objectHale != null) TestUtil.oclose(objectHale);
        }
    }
}