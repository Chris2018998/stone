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

import org.stone.shine.concurrent.locks.reentrantReadWriteLock.threads.ReadWriteLockAcquireThread;
import org.stone.shine.synchronizer.locks.ReentrantReadWriteLock;
import org.stone.test.TestCase;
import org.stone.test.TestUtil;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;

/**
 * ReentrantLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TryLockTest extends TestCase {

    public void test() throws Exception {
        //1: create lock and acquire in main thread
        Lock lock = new ReentrantReadWriteLock().writeLock();

        //2: create mock thread
        ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(lock, "tryLock");
        mockThread.start();

        try {
            //3: park main thread 1 second
            LockSupport.parkNanos(ParkDelayNanos);

            //4: check lock state
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, mockThread.getResult());
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, TestUtil.invokeMethod(lock, "isLocked"));
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", 1, TestUtil.invokeMethod(lock, "getHoldCount"));
        } finally {
            //5: unlock
            if (Objects.equals(mockThread.getResult(), true)) lock.unlock();
        }
    }
}
