/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.semaphore.threads;

import org.stone.shine.concurrent.ConcurrentMockThread;
import org.stone.shine.concurrent.Semaphore;

import java.util.concurrent.TimeUnit;

/**
 * Semaphore mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AcquireMockThread extends ConcurrentMockThread {
    private Semaphore semaphore;

    public AcquireMockThread(Semaphore semaphore, String methodName) {
        super(methodName);
        this.semaphore = semaphore;
    }

    public AcquireMockThread(Semaphore semaphore, String methodName, long timeout, TimeUnit timeUnit) {
        super(methodName, timeout, timeUnit);
        this.semaphore = semaphore;
    }

    public void run() {
        try {
            if ("acquire".equals(methodName)) {
                semaphore.acquire();
                this.result = true;
            } else if ("acquireUninterruptibly".equals(methodName)) {
                semaphore.acquireUninterruptibly();
                this.result = true;
            } else if ("tryAcquire".equals(methodName) && timeUnit != null) {
                this.result = semaphore.tryAcquire(timeout, timeUnit);
            } else if ("tryAcquire".equals(methodName)) {
                this.result = semaphore.tryAcquire();
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        }
    }
}
