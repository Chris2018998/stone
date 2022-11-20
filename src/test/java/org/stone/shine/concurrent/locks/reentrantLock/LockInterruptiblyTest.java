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

import org.stone.shine.concurrent.locks.reentrantLock.threads.LockAcquireThread;
import org.stone.shine.synchronizer.locks.ReentrantLock;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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

            //3: park main thread 1 second
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));

            //4: interrupt the mock thread
            mockThread.interrupt();

            //check InterruptedException in mock thread
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            if (mockThread.getInterruptedException() == null) TestUtil.assertError("mock thread not interrupted");
        } finally {
            lock.unlock();
        }
    }
}
