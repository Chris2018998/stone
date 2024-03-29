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

/**
 * CountDownLatch Mock Thread
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ZeroCountWaitThread extends BaseLatchThread {

    public ZeroCountWaitThread(CountDownLatch latch, String methodName) {
        super(latch, methodName);
    }

    public ZeroCountWaitThread(CountDownLatch latch, String methodName, long timeout, TimeUnit timeUnit) {
        super(latch, methodName, timeout, timeUnit);
    }

    public void run() {
        try {
            if ("await".equals(methodName) && timeUnit != null) {
                this.result = latch.await(timeout, timeUnit);
            } else if ("await".equals(methodName)) {
                latch.await();
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        }
    }
}
