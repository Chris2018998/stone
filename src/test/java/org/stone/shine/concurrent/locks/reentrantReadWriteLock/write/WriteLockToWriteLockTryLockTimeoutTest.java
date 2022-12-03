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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockToWriteLockTryLockTimeoutTest extends ReentrantReadWriteLockTestCase {

    public void test() throws Exception {
        boolean lockByMock = false;
        //1: lock by main thread
        writeLock.lock();

        //2: create mock thread
        ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(writeLock, "tryLock", 1, TimeUnit.SECONDS);
        mockThread.start();

        try {
            //3: park main thread 1 second
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

            //4: check writeLock state
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", false, mockThread.getResult());

            lockByMock = true;
        } finally {
            //5: unlock
            if (lockByMock) {
                mockThread.unlock();
            } else {
                writeLock.unlock();//unlock from main
            }
        }
    }
}
