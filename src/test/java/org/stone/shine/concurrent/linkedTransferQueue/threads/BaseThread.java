/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.linkedTransferQueue.threads;

import org.stone.shine.concurrent.ConcurrentMockThread;
import org.stone.shine.concurrent.LinkedTransferQueue;

import java.util.concurrent.TimeUnit;

/**
 * mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */

public class BaseThread extends ConcurrentMockThread {
    protected LinkedTransferQueue queue;

    public BaseThread(LinkedTransferQueue queue, String methodName) {
        super(methodName);
        this.queue = queue;
    }

    public BaseThread(LinkedTransferQueue queue, String methodName, long timeout, TimeUnit timeUnit) {
        super(methodName, timeout, timeUnit);
        this.queue = queue;
    }
}
