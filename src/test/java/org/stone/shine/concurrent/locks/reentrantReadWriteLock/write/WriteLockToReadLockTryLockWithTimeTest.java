/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.reentrantReadWriteLock.write;

import org.stone.base.TestUtil;
import org.stone.shine.concurrent.locks.reentrantReadWriteLock.ReadWriteLockAcquireThread;
import org.stone.shine.concurrent.locks.reentrantReadWriteLock.ReentrantReadWriteLockTestCase;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockToReadLockTryLockWithTimeTest extends ReentrantReadWriteLockTestCase {

    public void test() throws Exception {
        writeLock.lock();

        //2: create mock thread
        ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(readLock, "tryLock", Wait_Time, Wait_TimeUnit);
        mockThread.start();

        if (TestUtil.joinUtilWaiting(mockThread))
            writeLock.unlock();

        mockThread.join();
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, mockThread.getResult());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, TestUtil.invokeMethod(readLock, "isLocked"));
        //TestUtil.assertError("test failed,expect value:%s,actual value:%s", 1, TestUtil.invokeMethod(readLock, "getHoldCount"));
    }
}
