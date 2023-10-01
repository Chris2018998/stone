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

import java.util.concurrent.locks.LockSupport;

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.base.TestUtil.Wait_TimeUnit;

/**
 * ReentrantLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TryLockTest extends TestCase {

    public void test() throws Exception {
        //1: create lock and acquire in main thread
        ReentrantLock lock = new ReentrantLock();

        //2: create mock thread
        LockAcquireThread mockThread = new LockAcquireThread(lock, "tryLock");
        mockThread.start();

        //3: park main thread 1 second
        LockSupport.parkNanos(Wait_TimeUnit.toNanos(Wait_Time));

        //4: check lock state
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, mockThread.getResult());
        TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, lock.isLocked());
    }
}
