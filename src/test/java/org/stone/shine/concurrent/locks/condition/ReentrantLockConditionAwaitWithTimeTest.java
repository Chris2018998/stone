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
import org.stone.shine.concurrent.locks.condition.threads.ReentrantLockConditionAwaitThread;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.*;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockConditionAwaitWithTimeTest extends ReentrantLockConditionTestCase {

    public static void main(String[] args) throws Throwable {
        ReentrantLockConditionAwaitWithTimeTest test = new ReentrantLockConditionAwaitWithTimeTest();
        test.setUp();
        test.test();
    }

    public void test() throws Exception {
        //1:create wait thread
        ReentrantLockConditionAwaitThread awaitThread = new ReentrantLockConditionAwaitThread(lock, lockCondition, "await", Wait_Time, Wait_TimeUnit);
        awaitThread.start();

        //2:writeLock in main thread
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
        Boolean result = (Boolean) awaitThread.getResult();
        if (result) TestUtil.assertError("test failed,await timeout");
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked2());
    }
}
