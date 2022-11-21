/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.condition;

import java.util.concurrent.locks.Condition;

/**
 * condition test thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SignalThread extends BaseThread {
    private Exception signalException;

    public SignalThread(Condition condition, String methodName) {
        super(condition, methodName);
    }

    public void run() {
        try {
            if ("signal".equals(methodName) && timeUnit != null) {
                condition.signal();
            } else if ("await".equals(methodName)) {
                condition.signalAll();
            }
        } catch (Exception e) {
            this.signalException = e;
        }
    }

    public Exception getSignalException() {
        return signalException;
    }
}
