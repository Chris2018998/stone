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

import org.stone.shine.util.concurrent.LinkedTransferQueue;

import java.util.concurrent.TimeUnit;

/**
 * mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TransferThread extends BaseThread {
    private final Object offerObject;

    public TransferThread(LinkedTransferQueue queue, String methodName, Object offerObject) {
        super(queue, methodName);
        this.offerObject = offerObject;
    }

    public TransferThread(LinkedTransferQueue queue, String methodName, Object offerObject, long timeout, TimeUnit timeUnit) {
        super(queue, methodName, timeout, timeUnit);
        this.offerObject = offerObject;
    }

    public void run() {
        try {
            if ("tryTransfer".equals(methodName) && timeUnit != null) {
                this.result = queue.tryTransfer(offerObject, timeout, timeUnit);
            } else if ("tryTransfer".equals(methodName)) {
                this.result = queue.tryTransfer(offerObject);
            } else if ("transfer".equals(methodName)) {
                queue.transfer(offerObject);
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        }
    }
}
