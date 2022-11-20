/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.synchronousQueue.threads;

import org.stone.shine.concurrent.SynchronousQueue;

import java.util.concurrent.TimeUnit;

/**
 * mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */

public class BaseThread extends Thread {
    protected SynchronousQueue queue;
    protected String methodName;
    protected long timeout;
    protected TimeUnit unit;

    protected Object result;
    protected InterruptedException interruptedException;

    public BaseThread(SynchronousQueue queue, String methodName) {
        this.queue = queue;
        this.methodName = methodName;
    }

    public BaseThread(SynchronousQueue queue, String methodName, long timeout, TimeUnit unit) {
        this.queue = queue;
        this.methodName = methodName;
        this.timeout = timeout;
        this.unit = unit;
    }
}
