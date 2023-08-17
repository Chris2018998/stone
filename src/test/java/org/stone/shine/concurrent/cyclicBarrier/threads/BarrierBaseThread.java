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

import org.stone.shine.concurrent.ConcurrentMockThread;
import org.stone.shine.util.concurrent.CyclicBarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CountDownLatch Mock Thread runnable
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class BarrierBaseThread extends ConcurrentMockThread {

    CyclicBarrier barrier;

    TimeoutException timeoutException;

    BrokenBarrierException brokenBarrierException;

    BarrierBaseThread(CyclicBarrier barrier) {
        this.barrier = barrier;
    }

    BarrierBaseThread(CyclicBarrier barrier, String methodName) {
        super(methodName);
        this.barrier = barrier;
    }

    BarrierBaseThread(CyclicBarrier barrier, String methodName, long timeout, TimeUnit timeUnit) {
        super(methodName, timeout, timeUnit);
        this.barrier = barrier;
    }

    public TimeoutException getTimeoutException() {
        return timeoutException;
    }

    public BrokenBarrierException getBrokenBarrierException() {
        return brokenBarrierException;
    }
}
