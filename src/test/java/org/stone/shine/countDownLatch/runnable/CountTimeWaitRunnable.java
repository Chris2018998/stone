/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.countDownLatch.runnable;

import org.stone.shine.concurrent.CountDownLatch;

import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch Mock Thread runnable
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CountTimeWaitRunnable extends CountWaitRunnable {
    private long timeout;
    private TimeUnit unit;
    private boolean hasTimeout;

    public CountTimeWaitRunnable(CountDownLatch latch, long timeout, TimeUnit unit) {
        super(latch);
        this.timeout = timeout;
        this.unit = unit;
    }

    public void run() {
        try {
            this.hasTimeout = latch.await(timeout, unit);
        } catch (InterruptedException e) {
            cause = e;
        }
    }

    public boolean isHasTimeout() {
        return hasTimeout;
    }
}
