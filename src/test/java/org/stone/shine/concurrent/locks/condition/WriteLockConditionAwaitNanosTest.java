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

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * writeLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockConditionAwaitNanosTest extends WriteLockConditionTestCase {
    public void test() throws Exception {
        //1:create wait thread
        ReentrantWriteLockConditionAwaitThread awaitThread = new ReentrantWriteLockConditionAwaitThread(lock, lockCondition, "awaitNanos", Wait_TimeUnit.toNanos(Wait_Time), null);
        awaitThread.start();

        //3:check mock thread
        awaitThread.join();
        long remainTime = (long) awaitThread.getResult();
        if (remainTime > 0) TestUtil.assertError("test failed,await timeout");
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked2());
    }
}
