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
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.book.Book;

/**
 * Object borrow thread
 *
 * @author Chris Liao
 */
public class BaseThread extends Thread {
    protected String objectKey;
    protected BeeObjectSourceConfig<String, Book> config;
    protected BeeObjectSource<String, Book> objectSource;
    protected long targetTimeToRun;

    protected BeeObjectHandle<String, Book> objectHale;
    protected Throwable failureCause;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public BeeObjectSourceConfig<String, Book> getConfig() {
        return config;
    }

    public void setConfig(BeeObjectSourceConfig<String, Book> config) {
        this.config = config;
    }

    public BeeObjectSource<String, Book> getObjectSource() {
        return objectSource;
    }

    public void setObjectSource(BeeObjectSource<String, Book> objectSource) {
        this.objectSource = objectSource;
    }

    public long getTargetTimeToRun() {
        return targetTimeToRun;
    }

    public void setTargetTimeToRun(long targetTimeToRun) {
        this.targetTimeToRun = targetTimeToRun;
    }

    public BeeObjectHandle<String, Book> getObjectHale() {
        return objectHale;
    }

    public void setObjectHale(BeeObjectHandle<String, Book> objectHale) {
        this.objectHale = objectHale;
    }

    public Throwable getFailureCause() {
        return failureCause;
    }

    public void setFailureCause(Throwable failureCause) {
        this.failureCause = failureCause;
    }
}
