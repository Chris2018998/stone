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

import org.stone.shine.concurrent.locks.condition.threads.ReentrantWriteLockConditionAwaitThread;
import org.stone.test.TestUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Global_TimeoutNanos;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * lock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockConditionAwaitTimeoutTest extends WriteLockConditionTestCase {

    public void test() {
        //1:create wait thread
        ReentrantWriteLockConditionAwaitThread awaitThread = new ReentrantWriteLockConditionAwaitThread(lock, lockCondition, "await", ParkDelayNanos, TimeUnit.NANOSECONDS);
        awaitThread.start();

        //2:lock in main thread
        LockSupport.parkNanos(Global_TimeoutNanos);
        lock.lock();
        try {
            lockCondition.signal();
        } finally {
            lock.unlock();
        }

        //3:check mock thread
        LockSupport.parkNanos(ParkDelayNanos);
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.getResult());//timeout
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked2());
    }
}
