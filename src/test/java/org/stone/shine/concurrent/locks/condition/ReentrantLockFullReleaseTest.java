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

import org.stone.base.TestUtil;
import org.stone.shine.concurrent.ConcurrentTimeUtil;
import org.stone.shine.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.locks.Condition;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkNanos;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockFullReleaseTest extends ReentrantLockConditionTestCase {
    public static void main(String[] args) throws Throwable {
        ReentrantLockFullReleaseTest test = new ReentrantLockFullReleaseTest();
        test.setUp();
        test.test();
    }

    public void test() throws Exception {
        //1:create wait thread
        ReentrantLockConditionAwaitThread awaitThread = new ReentrantLockConditionAwaitThread(lock, lockCondition);
        awaitThread.start();

        if (ConcurrentTimeUtil.isInWaiting(awaitThread, ParkNanos)) {
            lock.lock();
            try {
                lockCondition.signal();
            } finally {
                lock.unlock();
            }
        }

        //3:check mock thread
        awaitThread.join();
        if (awaitThread.getAssertionError() != null) TestUtil.assertError("test failed");
    }

    private static class ReentrantLockConditionAwaitThread extends Thread {
        private AssertionError assertionError;
        private InterruptedException interruptedException;
        private ReentrantLock lock;
        private Condition condition;

        ReentrantLockConditionAwaitThread(ReentrantLock lock, Condition condition) {
            this.lock = lock;
            this.condition = condition;
        }

        AssertionError getAssertionError() {
            return assertionError;
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
