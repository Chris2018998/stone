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

import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_Time;
import static org.stone.shine.concurrent.ConcurrentTimeUtil.Wait_TimeUnit;

/**
 * ReentrantLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TryLockTimeoutTest extends TestCase {

    public void test() throws Exception {
        boolean lockByMock = false;
        //1: create lock and acquire in main thread
        ReentrantLock lock = new ReentrantLock();
        lock.lock();

        //2: create mock thread
        LockAcquireThread mockThread = new LockAcquireThread(lock, "tryLock", Wait_Time, Wait_TimeUnit);
        mockThread.start();

        try {
            mockThread.join();
            //4: check lock state
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", false, mockThread.getResult());
        } finally {
            lock.unlock();//unlock from main
        }
    }
}
