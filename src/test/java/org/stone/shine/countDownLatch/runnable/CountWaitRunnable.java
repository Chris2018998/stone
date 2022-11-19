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

/**
 * CountDownLatch Mock Thread runnable
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class CountWaitRunnable extends BaseRunnable {

    protected InterruptedException cause;

    public CountWaitRunnable(CountDownLatch latch) {
        super(latch);
    }

    public void run() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            cause = e;
        }
    }

    public InterruptedException getCause() {
        return cause;
    }
}
