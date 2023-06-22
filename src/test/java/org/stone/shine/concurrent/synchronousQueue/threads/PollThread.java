/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.synchronousQueue.threads;

import org.stone.shine.util.concurrent.SynchronousQueue;

import java.util.concurrent.TimeUnit;

/**
 * mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PollThread extends BaseThread {

    public PollThread(SynchronousQueue queue, String methodName) {
        super(queue, methodName);
    }

    public PollThread(SynchronousQueue queue, String methodName, long timeout, TimeUnit timeUnit) {
        super(queue, methodName, timeout, timeUnit);
    }

    public void run() {
        try {
            if ("poll".equals(methodName) && timeUnit != null) {
                this.result = queue.poll(timeout, timeUnit);
            } else if ("poll".equals(methodName)) {
                this.result = queue.poll();
            } else if ("take".equals(methodName)) {
                this.result = queue.take();
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        }
    }
}

