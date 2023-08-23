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

import java.util.concurrent.locks.LockSupport;

/**
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockToReadLockLockUninterruptedTest extends ReentrantReadWriteLockTestCase {

    public static void main(String[] arg) throws Throwable {
        WriteLockToReadLockLockUninterruptedTest test = new WriteLockToReadLockLockUninterruptedTest();
        test.setUp();
        test.test();
    }

    public void test() throws Exception {
        //1: create lock and acquire in main thread
        boolean isUnlock = false;
        writeLock.lock();

        try {
            //2: create mock thread
            ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(readLock, "lock");
            mockThread.start();

            if (TestUtil.joinUtilWaiting(mockThread)) {
                mockThread.interrupt();
                LockSupport.parkNanos(100L);
                if (mockThread.getState() != Thread.State.WAITING) TestUtil.assertError("mock thread not in waiting");

                writeLock.unlock();
                isUnlock = true;
            }

            //7: check mock thead
            mockThread.join();
            TestUtil.assertError("Lock test fail, expect value:%s,actual value:%s", true, mockThread.getResult());
        } finally {
            if (!isUnlock) writeLock.unlock();
        }

    }
}
