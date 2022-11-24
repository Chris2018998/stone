/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.condition.threads;

import org.stone.shine.concurrent.ConcurrentMockThread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * condition test thread
 *
 * @author Chris Liao
 * @version 1.0
 */

class BaseThread extends ConcurrentMockThread {
    protected Condition condition;

    BaseThread(Condition condition, String methodName) {
        super(methodName);
        this.condition = condition;
    }

    BaseThread(Condition condition, String methodName, long timeout, TimeUnit timeUnit) {
        super(methodName, timeout, timeUnit);
        this.condition = condition;
    }
}
