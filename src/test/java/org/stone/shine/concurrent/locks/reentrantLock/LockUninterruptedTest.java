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

import org.stone.shine.synchronizer.locks.ReentrantLock;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * ReentrantLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class LockUninterruptedTest extends TestCase {

    public void test() throws Exception {
        //1: create lock and acquire in main thread
        boolean isUnlock = false;
        ReentrantLock lock = new ReentrantLock();
        lock.lock();

        try {
            //2: create mock thread
            LockAcquireThread mockThread = new LockAcquireThread(lock, "lock");
            mockThread.start();

            //3: park main thread 1 second
            LockSupport.parkNanos(ParkDelayNanos);

            //4: interrupt the mock thread
            mockThread.interrupt();

            //5: park the main thread 1 second and check mock state
            LockSupport.parkNanos(ParkDelayNanos);
            if (mockThread.getState() != Thread.State.WAITING) TestUtil.assertError("mock thread not in waiting");

            //6: unlock from main
            lock.unlock();
            isUnlock = true;

            //7: check mock thead
            LockSupport.parkNanos(ParkDelayNanos);
            TestUtil.assertError("Lock test fail, expect value:%s,actual value:%s", true, mockThread.getResult());
        } finally {
            if (!isUnlock) lock.unlock();
        }
    }
}
