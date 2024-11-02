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

/**
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ReadLockShareAcquireTest extends ReentrantReadWriteLockTestCase {

    public void test() throws Exception {
        //1:lock by main thread
        readLock.lock();
        try {
            //2: create mock thread
            ReadWriteLockAcquireThread mockThread1 = new ReadWriteLockAcquireThread(readLock, "lock");
            ReadWriteLockAcquireThread mockThread2 = new ReadWriteLockAcquireThread(readLock, "lock");
            mockThread1.start();
            mockThread2.start();

            boolean test1 = TestUtil.waitUtilWaiting(mockThread1);
            boolean test2 = TestUtil.waitUtilWaiting(mockThread2);

            if (test1 && test2)
                TestUtil.assertError("test failed,expect value:%s,actual value:%s", 3, TestUtil.invokeMethod(readLock, "getHoldCount"));
        } finally {
            readLock.unlock();
        }
    }
}
