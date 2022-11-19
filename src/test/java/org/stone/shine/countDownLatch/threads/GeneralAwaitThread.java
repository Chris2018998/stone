/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.countDownLatch.threads;

import org.stone.shine.concurrent.CountDownLatch;

import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch Mock Thread
 *
 * @author Chris Liao
 * @version 1.0
 */

public class GeneralAwaitThread extends BaseThread {
    private long timeout;
    private TimeUnit timeUnit;
    private InterruptedException interruptedException;

    public GeneralAwaitThread(CountDownLatch latch) {
        super(latch);
    }

    public GeneralAwaitThread(CountDownLatch latch, long timeout, TimeUnit timeUnit) {
        super(latch);
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public InterruptedException getInterruptedException() {
        return interruptedException;
    }

    public void run() {
        if (timeout == 0) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                this.interruptedException = e;
            }
        } else {
            try {
                latch.await(timeout, timeUnit);
            } catch (InterruptedException e) {
                this.interruptedException = e;
            }
        }
    }
}
