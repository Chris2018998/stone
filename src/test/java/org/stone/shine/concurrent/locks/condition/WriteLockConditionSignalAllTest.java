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
import org.stone.shine.concurrent.locks.condition.threads.ReentrantWriteLockConditionAwaitThread;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkNanos;

/**
 * writeLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockConditionSignalAllTest extends WriteLockConditionTestCase {

    public void test() throws Exception {
        //1:create wait thread
        ReentrantWriteLockConditionAwaitThread waitThread1 = new ReentrantWriteLockConditionAwaitThread(lock, lockCondition, "await");
        waitThread1.start();
        ReentrantWriteLockConditionAwaitThread waitThread2 = new ReentrantWriteLockConditionAwaitThread(lock, lockCondition, "await");
        waitThread2.start();

        //2:writeLock in main thread
        boolean test1 = ConcurrentTimeUtil.isInWaiting(waitThread1, ParkNanos);
        boolean test2 = ConcurrentTimeUtil.isInWaiting(waitThread2, ParkNanos);
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
