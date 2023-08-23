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

public class ReadLockToWriteLockInterruptiblyTest extends ReentrantReadWriteLockTestCase {
    public void test() throws Exception {

        readLock.lock();
        try {
            //2: create mock thread
            ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(writeLock, "lockInterruptibly");
            mockThread.start();

            if (TestUtil.joinUtilWaiting(mockThread)) {
                mockThread.interrupt();
            }

            mockThread.join();
            if (mockThread.getInterruptedException() == null) TestUtil.assertError("mock thread not interrupted");
        } finally {
            readLock.unlock();
        }
    }
}
