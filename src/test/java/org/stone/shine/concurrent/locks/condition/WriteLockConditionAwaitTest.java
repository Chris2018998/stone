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
import org.stone.shine.concurrent.locks.condition.threads.ReentrantWriteLockConditionAwaitThread;

/**
 * writeLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockConditionAwaitTest extends WriteLockConditionTestCase {

    public void test() throws Exception {
        //1:create wait thread
        ReentrantWriteLockConditionAwaitThread awaitThread = new ReentrantWriteLockConditionAwaitThread(lock, lockCondition, "await");
        awaitThread.start();

        //2:writeLock in main thread
        if (TestUtil.waitUtilWaiting(awaitThread)) {
            lock.lock();
            try {
                lockCondition.signal();
            } finally {
                lock.unlock();
            }
        }

        //3:check time
        awaitThread.join();
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked2());
    }
}
