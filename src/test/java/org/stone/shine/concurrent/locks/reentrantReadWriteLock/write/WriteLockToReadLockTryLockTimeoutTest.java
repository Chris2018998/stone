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

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_Time;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_TimeUnit;

/**
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockToReadLockTryLockTimeoutTest extends ReentrantReadWriteLockTestCase {

    public void test() throws Exception {
        boolean lockByMock = false;
        writeLock.lock();

        //2: create mock thread
        ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(readLock, "tryLock", Wait_Time, Wait_TimeUnit);
        mockThread.start();

        try {
            mockThread.join();
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", false, mockThread.getResult());
        } finally {
            writeLock.unlock();//unlock from main
        }
    }
}
