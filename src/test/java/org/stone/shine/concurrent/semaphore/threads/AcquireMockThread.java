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

import org.stone.shine.concurrent.Semaphore;

import java.util.concurrent.TimeUnit;

/**
 * Semaphore mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AcquireMockThread extends Thread {
    private Semaphore semaphore;
    private String acquireMethodName;
    private long timeout;
    private TimeUnit unit;
    private boolean acquireSuccess;
    private InterruptedException interruptedException;

    public AcquireMockThread(Semaphore semaphore, String acquireMethodName) {
        this.semaphore = semaphore;
        this.acquireMethodName = acquireMethodName;
    }

    public AcquireMockThread(Semaphore semaphore, String acquireMethodName, long timeout, TimeUnit unit) {
        this.semaphore = semaphore;
        this.acquireMethodName = acquireMethodName;
        this.timeout = timeout;
        this.unit = unit;
    }

    public boolean isAcquireSuccess() {
        return acquireSuccess;
    }

    public InterruptedException getInterruptedException() {
        return interruptedException;
    }

    public void run() {
        try {
            if ("acquire".equals(acquireMethodName)) {
                semaphore.acquire();
                this.acquireSuccess = true;
            } else if ("acquireUninterruptibly".equals(acquireMethodName)) {
                semaphore.acquireUninterruptibly();
                this.acquireSuccess = true;
            } else if ("tryAcquire".equals(acquireMethodName) && unit != null) {
                this.acquireSuccess = semaphore.tryAcquire(timeout, unit);
            } else if ("tryAcquire".equals(acquireMethodName)) {
                this.acquireSuccess = semaphore.tryAcquire();
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        }
    }
}
