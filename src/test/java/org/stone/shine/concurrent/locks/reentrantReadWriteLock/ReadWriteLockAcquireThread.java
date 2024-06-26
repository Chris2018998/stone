/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.reentrantReadWriteLock;

import org.stone.shine.concurrent.ConcurrentMockThread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.stone.tools.CommonUtil.objectEquals;

/**
 * mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ReadWriteLockAcquireThread extends ConcurrentMockThread {
    private final Lock lock;

    public ReadWriteLockAcquireThread(Lock lock, String methodName) {
        super(methodName);
        this.lock = lock;
    }

    public ReadWriteLockAcquireThread(Lock lock, String methodName, long timeout, TimeUnit timeUnit) {
        super(methodName, timeout, timeUnit);
        this.lock = lock;
    }

    public void run() {
        try {
            if ("tryLock".equals(methodName) && timeUnit != null) {
                this.result = lock.tryLock(timeout, timeUnit);
            } else if ("tryLock".equals(methodName)) {
                this.result = lock.tryLock();
            } else if ("lock".equals(methodName)) {
                lock.lock();
                this.result = true;
            } else if ("lockInterruptibly".equals(methodName)) {
                lock.lockInterruptibly();
                this.result = true;
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        }
    }

    public void unlock() {
        if (objectEquals(result, true)) {
            lock.unlock();
            this.result = false;
        }
    }
}
