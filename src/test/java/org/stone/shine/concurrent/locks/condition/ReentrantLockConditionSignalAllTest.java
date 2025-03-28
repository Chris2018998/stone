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
import org.stone.shine.concurrent.locks.condition.threads.ReentrantLockConditionAwaitThread;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockConditionSignalAllTest extends ReentrantLockConditionTestCase {

    public static void main(String[] args) throws Throwable {
        ReentrantLockConditionSignalAllTest test = new ReentrantLockConditionSignalAllTest();
        test.setUp();
        test.test();
    }

    public void test() throws Exception {
        //1:create wait thread
        ReentrantLockConditionAwaitThread waitThread1 = new ReentrantLockConditionAwaitThread(lock, lockCondition, "await");
        waitThread1.start();
        ReentrantLockConditionAwaitThread waitThread2 = new ReentrantLockConditionAwaitThread(lock, lockCondition, "await");
        waitThread2.start();

        //2:writeLock in main thread
        boolean test1 = TestUtil.waitUtilWaiting(waitThread1);
        boolean test2 = TestUtil.waitUtilWaiting(waitThread2);
        if (test1 && test2) {
            lock.lock();
            try {
                lockCondition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        //3:check writeLock state
        waitThread1.join();
        waitThread2.join();
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread1.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread1.isLocked2());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread2.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread2.isLocked2());
    }
}
