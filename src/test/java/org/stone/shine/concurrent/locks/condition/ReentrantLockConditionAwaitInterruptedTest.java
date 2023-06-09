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

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockConditionAwaitInterruptedTest extends ReentrantLockConditionTestCase {

    public void test() {
        //1:create wait thread
        ReentrantLockConditionAwaitThread awaitThread = new ReentrantLockConditionAwaitThread(lock, lockCondition, "await");
        awaitThread.start();

        //2:writeLock in main thread
        LockSupport.parkNanos(ParkDelayNanos);
        awaitThread.interrupt();

        //3:writeLock for main thread
        lock.lock();
        try {
            lockCondition.signal();
        } finally {
            lock.unlock();
        }

        //4:check time
        LockSupport.parkNanos(ParkDelayNanos);
        if (awaitThread.getInterruptedException() == null) TestUtil.assertError("test failed");
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked2());
    }
}
