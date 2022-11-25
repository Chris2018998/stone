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

import org.stone.shine.concurrent.LinkedTransferQueue;

import java.util.concurrent.TimeUnit;

/**
 * mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class OfferThread extends BaseThread {
    private Object offerObject;

    public OfferThread(LinkedTransferQueue queue, String methodName, Object offerObject) {
        super(queue, methodName);
        this.offerObject = offerObject;
    }

    public OfferThread(LinkedTransferQueue queue, String methodName, Object offerObject, long timeout, TimeUnit timeUnit) {
        super(queue, methodName, timeout, timeUnit);
        this.offerObject = offerObject;
    }

    public void run() {
        if ("offer".equals(methodName) && timeUnit != null) {
            this.result = queue.offer(offerObject, timeout, timeUnit);
        } else if ("offer".equals(methodName)) {
            this.result = queue.offer(offerObject);
        } else if ("put".equals(methodName)) {
            queue.put(offerObject);
        }
    }
}
