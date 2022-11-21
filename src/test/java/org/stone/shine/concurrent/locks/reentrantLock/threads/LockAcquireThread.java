/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.reentrantLock.threads;

import org.stone.shine.concurrent.ConcurrentMockThread;
import org.stone.shine.synchronizer.locks.ReentrantLock;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * mock thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class LockAcquireThread extends ConcurrentMockThread {
    private Lock lock;

    public LockAcquireThread(Lock lock, String methodName) {
        super(methodName);
        this.lock = lock;
        this.result = false;
    }

    public LockAcquireThread(ReentrantLock lock, String methodName, long timeout, TimeUnit timeUnit) {
        super(methodName, timeout, timeUnit);
        this.lock = lock;
        this.result = false;
    }

    public void run() {
        try {
            if ("lock".equals(methodName)) {
                lock.lock();
                this.result = true;
            } else if ("lockInterruptibly".equals(methodName)) {
                lock.lockInterruptibly();
                this.result = true;
            } else if ("tryLock".equals(methodName) && timeUnit != null) {
                this.result = lock.tryLock(timeout, timeUnit);
            } else if ("tryLock".equals(methodName)) {
                this.result = lock.tryLock();
            }
        } catch (InterruptedException e) {
            this.interruptedException = e;
        }
    }

    public void unlock() {
        if (Objects.equals(result, true)) {
            lock.unlock();
            this.result = false;
        }
    }
}
