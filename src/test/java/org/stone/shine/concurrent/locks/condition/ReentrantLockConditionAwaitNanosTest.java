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

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_Time;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_TimeUnit;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockConditionAwaitNanosTest extends ReentrantLockConditionTestCase {

    public static void main(String[] args) throws Throwable {
        ReentrantLockConditionAwaitNanosTest test = new ReentrantLockConditionAwaitNanosTest();
        test.setUp();
        test.test();
    }

    public void test() throws Exception {
        //1:create wait thread
        ReentrantLockConditionAwaitThread awaitThread = new ReentrantLockConditionAwaitThread(lock, lockCondition, "awaitNanos", Wait_TimeUnit.toNanos(Wait_Time), null);
        awaitThread.start();

        //3:check mock thread
        awaitThread.join();
        long remainTime = (long) awaitThread.getResult();
        if (remainTime > 0) TestUtil.assertError("test failed,await timeout");
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked2());
    }
}
