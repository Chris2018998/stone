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

import org.stone.shine.synchronizer.locks.ReentrantLock;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * condition test thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ReentrantLockConditionAwaitThread extends BaseThread {
    private ReentrantLock lock;
    private Date deadline;
    private boolean locked1;
    private boolean locked2;

    public ReentrantLockConditionAwaitThread(ReentrantLock lock, Condition condition, String methodName) {
        super(condition, methodName);
        this.lock = lock;
    }

    public ReentrantLockConditionAwaitThread(ReentrantLock lock, Condition condition, String methodName, Date deadline) {
        super(condition, methodName);
        this.deadline = deadline;
        this.lock = lock;
    }

    public ReentrantLockConditionAwaitThread(ReentrantLock lock, Condition condition, String methodName, long timeout, TimeUnit timeUnit) {
        super(condition, methodName, timeout, timeUnit);
        this.lock = lock;
    }

    public boolean isLocked1() {
        return locked1;
    }

    public boolean isLocked2() {
        return locked2;
    }

    public void run() {
        lock.lock();
        try {
            locked1 = lock.isHeldByCurrentThread();
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
            locked2 = lock.isHeldByCurrentThread();
        } catch (InterruptedException e) {
            this.interruptedException = e;
        } finally {
            lock.unlock();
        }
    }
}
