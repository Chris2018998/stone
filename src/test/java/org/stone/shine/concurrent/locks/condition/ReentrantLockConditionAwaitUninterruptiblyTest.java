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

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockConditionAwaitUninterruptiblyTest extends ReentrantLockConditionTestCase {

    public static void main(String[] args) throws Throwable {
        ReentrantLockConditionAwaitUninterruptiblyTest test = new ReentrantLockConditionAwaitUninterruptiblyTest();
        test.setUp();
        test.test();
    }

    public void test() throws Exception {
        //1:create wait thread
        ReentrantLockConditionAwaitThread awaitThread = new ReentrantLockConditionAwaitThread(lock, lockCondition, "awaitUninterruptibly");
        awaitThread.start();

        if (TestUtil.waitUtilWaiting(awaitThread)) {
            awaitThread.interrupt();
            LockSupport.parkNanos(100L);
            if (awaitThread.getState() != Thread.State.WAITING) TestUtil.assertError("mock thread not in waiting");
        }

        lock.lock();
        try {
            lockCondition.signal();
        } finally {
            lock.unlock();
        }

        if (awaitThread.getInterruptedException() != null) TestUtil.assertError("test failed");
    }
}
