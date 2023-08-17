/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.countDownLatch.threads;

import org.stone.shine.concurrent.ConcurrentMockThread;
import org.stone.shine.util.concurrent.CountDownLatch;

import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch Mock Thread runnable
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class BaseLatchThread extends ConcurrentMockThread {

    protected CountDownLatch latch;

    BaseLatchThread(CountDownLatch latch) {
        this.latch = latch;
    }

    BaseLatchThread(CountDownLatch latch, String methodName) {
        super(methodName);
        this.latch = latch;
    }

    BaseLatchThread(CountDownLatch latch, String methodName, long timeout, TimeUnit timeUnit) {
        super(methodName, timeout, timeUnit);
        this.latch = latch;
    }
}
