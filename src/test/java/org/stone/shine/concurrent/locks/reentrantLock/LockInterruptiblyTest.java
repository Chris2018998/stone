/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.reentrantLock;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class LockInterruptiblyTest extends TestCase {
    public void test() throws Exception {
        //1: create lock and acquire in main thread
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            //2: create mock thread
            LockAcquireThread mockThread = new LockAcquireThread(lock, "lockInterruptibly");
            mockThread.start();

            if (TestUtil.joinUtilWaiting(mockThread)) {
                mockThread.interrupt();
            }

            mockThread.join();
            if (mockThread.getInterruptedException() == null) TestUtil.assertError("mock thread not interrupted");
        } finally {
            lock.unlock();
        }
    }
}
