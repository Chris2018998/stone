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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockFullReleaseTest extends WriteLockConditionTestCase {

    public void test() throws Exception {
        //1:create wait thread
        ReentrantLockConditionAwaitThread awaitThread = new ReentrantLockConditionAwaitThread(lock, lockCondition);
        awaitThread.start();
        if (TestUtil.joinUtilWaiting(awaitThread)) {
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
        private Lock lock;
        private Condition lockCondition;

        ReentrantLockConditionAwaitThread(Lock lock, Condition lockCondition) {
            this.lock = lock;
            this.lockCondition = lockCondition;
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
                        TestUtil.assertError("test failed,expect value:%s,actual value:%s", 3, TestUtil.invokeMethod(lock, "getHoldCount"));
                        lockCondition.await();
                        TestUtil.assertError("test failed,expect value:%s,actual value:%s", 3, TestUtil.invokeMethod(lock, "getHoldCount"));
                    } finally {
                        lock.unlock();
                    }
                    TestUtil.assertError("test failed,expect value:%s,actual value:%s", 2, TestUtil.invokeMethod(lock, "getHoldCount"));
                } finally {
                    lock.unlock();
                }
                TestUtil.assertError("test failed,expect value:%s,actual value:%s", 1, TestUtil.invokeMethod(lock, "getHoldCount"));
            } catch (InterruptedException e) {
                this.interruptedException = e;
            } catch (AssertionError e) {
                this.assertionError = e;
            } finally {
                try {
                    lock.unlock();
                    TestUtil.assertError("test failed,expect value:%s,actual value:%s", 0, TestUtil.invokeMethod(lock, "getHoldCount"));
                } catch (AssertionError e) {
                    this.assertionError = e;
                }
            }
        }
    }
}
