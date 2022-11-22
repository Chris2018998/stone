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

import org.stone.shine.concurrent.locks.condition.threads.ReentrantLockConditionAwaitThread;
import org.stone.test.TestUtil;

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * lock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockConditionSignalAllTest extends ReentrantLockConditionTestCase {

    public void test() throws Exception {
        //1:create wait thread
        ReentrantLockConditionAwaitThread waitThread1 = new ReentrantLockConditionAwaitThread(lock, lockCondition, "await");
        waitThread1.start();
        ReentrantLockConditionAwaitThread waitThread2 = new ReentrantLockConditionAwaitThread(lock, lockCondition, "await");
        waitThread2.start();

        //2:lock in main thread
        LockSupport.parkNanos(ParkDelayNanos);
        try {
            lock.lock();
            lockCondition.signalAll();
        } finally {
            lock.unlock();
        }

        //3:check lock state
        LockSupport.parkNanos(ParkDelayNanos);
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread1.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread1.isLocked2());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread2.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, waitThread2.isLocked2());
    }
}
