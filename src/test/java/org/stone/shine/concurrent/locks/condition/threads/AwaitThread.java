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

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * condition test thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AwaitThread extends BaseThread {
    private Lock lock;
    private boolean enterLock;
    private long awaitOverTime;
    private Date deadline;

    public AwaitThread(Lock lock, Condition condition, String methodName) {
        super(condition, methodName);
        this.lock = lock;
    }

    public AwaitThread(Lock lock, Condition condition, String methodName, Date deadline) {
        super(condition, methodName);
        this.deadline = deadline;
        this.lock = lock;
    }

    public AwaitThread(Lock lock, Condition condition, String methodName, long timeout, TimeUnit timeUnit) {
        super(condition, methodName, timeout, timeUnit);
    }

    public boolean isEnterLock() {
        return enterLock;
    }

    public long getAwaitOverTime() {
        return awaitOverTime;
    }

    public void run() {
        lock.lock();
        try {
            this.enterLock = true;
            if ("await".equals(methodName) && timeUnit != null) {
                this.result = condition.await(timeout, timeUnit);
            } else if ("await".equals(methodName)) {
                condition.await();
            } else if ("awaitUninterruptibly".equals(methodName)) {
                condition.awaitUninterruptibly();
            } else if ("awaitNanos".equals(methodName)) {
                this.result = condition.awaitNanos(timeout);
            } else if ("awaitUntil".equals(methodName)) {
                this.result = condition.awaitUntil(deadline);
            }
            this.awaitOverTime = System.currentTimeMillis();
        } catch (InterruptedException e) {
            this.interruptedException = e;
        } finally {
            lock.unlock();
        }
    }
}
