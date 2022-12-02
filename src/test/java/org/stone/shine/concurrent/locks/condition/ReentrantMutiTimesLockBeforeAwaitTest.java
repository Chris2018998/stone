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

import org.stone.shine.synchronizer.locks.ReentrantLock;
import org.stone.test.TestUtil;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantMutiTimesLockBeforeAwaitTest extends ReentrantLockConditionTestCase {

    public void test() {
        //1:create wait thread
        ReentrantLockConditionAwaitThread awaitThread = new ReentrantLockConditionAwaitThread(lock, lockCondition);
        awaitThread.start();

        //2:writeLock in main thread
        LockSupport.parkNanos(ParkDelayNanos);
        lock.lock();
        try {
            lockCondition.signal();
        } finally {
            lock.unlock();
        }

        //3:check mock thread
        LockSupport.parkNanos(ParkDelayNanos);
        if (awaitThread.assertionError != null) TestUtil.assertError("test failed");
    }

    private static class ReentrantLockConditionAwaitThread extends Thread {
        protected Object result;
        protected AssertionError assertionError;
        protected InterruptedException interruptedException;
        private ReentrantLock lock;
        private Condition condition;

        public ReentrantLockConditionAwaitThread(ReentrantLock lock, Condition condition) {
            this.lock = lock;
            this.condition = condition;
        }

        public Object getResult() {
            return result;
        }

        public InterruptedException getInterruptedException() {
            return interruptedException;
        }

        public void run() {
            lock.lock();
            try {
                lock.lock();
                try {
                    lock.lock();
                    try {
                        TestUtil.assertError("test failed,expect value:%s,actual value:%s", 3, lock.getHoldCount());
                        condition.await();
                        TestUtil.assertError("test failed,expect value:%s,actual value:%s", 3, lock.getHoldCount());
                    } finally {
                        lock.unlock();
                    }
                    TestUtil.assertError("test failed,expect value:%s,actual value:%s", 2, lock.getHoldCount());
                } finally {
                    lock.unlock();
                }
                TestUtil.assertError("test failed,expect value:%s,actual value:%s", 1, lock.getHoldCount());
            } catch (InterruptedException e) {
                this.interruptedException = e;
            } catch (AssertionError e) {
                this.assertionError = e;
            } finally {
                try {
                    lock.unlock();
                    TestUtil.assertError("test failed,expect value:%s,actual value:%s", 0, lock.getHoldCount());
                } catch (AssertionError e) {
                    this.assertionError = e;
                }
            }
        }
    }
}
