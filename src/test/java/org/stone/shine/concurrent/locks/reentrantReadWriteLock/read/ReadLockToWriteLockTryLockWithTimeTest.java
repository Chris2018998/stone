/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.reentrantReadWriteLock.read;

import org.stone.base.TestUtil;
import org.stone.shine.concurrent.locks.reentrantReadWriteLock.ReadWriteLockAcquireThread;
import org.stone.shine.concurrent.locks.reentrantReadWriteLock.ReentrantReadWriteLockTestCase;

import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReadLockToWriteLockTryLockWithTimeTest extends ReentrantReadWriteLockTestCase {

    public void test() throws Exception {
        boolean lockByMock = false;
        //lock by main thread
        readLock.lock();

        //2:create mock thread
        ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(writeLock, "tryLock", Wait_Time, Wait_TimeUnit);
        mockThread.start();

        try {
            if (TestUtil.joinUtilWaiting(mockThread)) {
                readLock.unlock();
            }

            //5: check writeLock state
            LockSupport.parkNanos(Wait_Time);
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, mockThread.getResult());
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, TestUtil.invokeMethod(writeLock, "isLocked"));
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", 1, TestUtil.invokeMethod(writeLock, "getHoldCount"));

            lockByMock = true;
        } finally {
            //6: unlock
            if (lockByMock) {
                mockThread.unlock();
            } else {
                readLock.unlock();//unlock from main
            }
        }
    }
}
