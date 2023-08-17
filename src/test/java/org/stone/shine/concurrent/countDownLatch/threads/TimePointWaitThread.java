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

import org.stone.shine.util.concurrent.CountDownLatch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * CountDownLatch Mock Thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TimePointWaitThread extends ZeroCountWaitThread {
    private long concurrentTimePoint;

    public TimePointWaitThread(CountDownLatch latch, long concurrentTimePoint, String methodName) {
        super(latch, methodName);
        this.concurrentTimePoint = concurrentTimePoint;
    }

    public TimePointWaitThread(CountDownLatch latch, long concurrentTimePoint, String methodName, long timeout, TimeUnit timeUnit) {
        super(latch, methodName, timeout, timeUnit);
        this.concurrentTimePoint = concurrentTimePoint;
    }

    public void run() {
        LockSupport.parkNanos(concurrentTimePoint - System.nanoTime());
        super.run();
    }
}
