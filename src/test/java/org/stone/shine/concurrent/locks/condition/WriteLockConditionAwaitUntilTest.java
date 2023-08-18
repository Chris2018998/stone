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

import java.util.Date;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_Time;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_TimeUnit;

/**
 * writeLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockConditionAwaitUntilTest extends WriteLockConditionTestCase {

    public void test() throws Exception {
        //1:create wait thread
        Date deadline = new Date(System.currentTimeMillis() + Wait_TimeUnit.toMillis(Wait_Time));
        ReentrantWriteLockConditionAwaitThread awaitThread = new ReentrantWriteLockConditionAwaitThread(lock, lockCondition, "awaitUntil", deadline);
        awaitThread.start();

        //2:writeLock in main thread
        awaitThread.join();
        Boolean result = (Boolean) awaitThread.getResult();
        if (!result) TestUtil.assertError("test failed,await timeout");
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked1());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, awaitThread.isLocked2());
    }
}
