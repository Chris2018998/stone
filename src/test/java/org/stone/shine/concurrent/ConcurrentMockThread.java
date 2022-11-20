/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * synchronousQueue Test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class ConcurrentMockThread extends Thread {
    //parameters
    protected String methodName;
    protected long timeout;
    protected TimeUnit timeUnit;

    //response（result/exception）
    protected Object result;
    protected InterruptedException interruptedException;

    public ConcurrentMockThread() {
    }

    public ConcurrentMockThread(String methodName) {
        this.methodName = methodName;
    }

    public ConcurrentMockThread(String methodName, long timeout, TimeUnit timeUnit) {
        this.methodName = methodName;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public Object getResult() {
        return result;
    }

    public InterruptedException getInterruptedException() {
        return interruptedException;
    }
}
