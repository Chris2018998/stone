/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.cyclicBarrier.threads;

import org.stone.shine.util.concurrent.CyclicBarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CountDownLatch Mock Thread
 *
 * @author Chris Liao
 * @version 1.0
 */

public class BarrierAwaitThread extends BarrierBaseThread {

    public BarrierAwaitThread(CyclicBarrier barrier, String methodName) {
        super(barrier, methodName);
    }

    public BarrierAwaitThread(CyclicBarrier barrier, String methodName, long timeout, TimeUnit timeUnit) {
        super(barrier, methodName, timeout, timeUnit);
    }

    public void run() {
        try {
            if ("await".equals(methodName) && timeUnit != null) {
                this.result = barrier.await(timeout, timeUnit);
            } else if ("await".equals(methodName)) {
                this.result = barrier.await();
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        } catch (BrokenBarrierException e) {
            this.brokenBarrierException = e;
        } catch (TimeoutException e) {
            this.timeoutException = e;
        }
    }
}
