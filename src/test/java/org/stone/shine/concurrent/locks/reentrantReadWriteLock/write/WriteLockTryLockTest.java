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

import static org.stone.base.TestUtil.Wait_Time;
import static org.stone.tools.CommonUtil.objectEquals;

/**
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */

public class WriteLockTryLockTest extends ReentrantReadWriteLockTestCase {

    public void test() throws Exception {
        //2: create mock thread
        ReadWriteLockAcquireThread mockThread = new ReadWriteLockAcquireThread(writeLock, "tryLock");
        mockThread.start();

        try {
            //3: park main thread 1 second
            LockSupport.parkNanos(Wait_Time);

            //4: check writeLock state
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, mockThread.getResult());
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, TestUtil.invokeMethod(writeLock, "isLocked"));
        } finally {
            //5: unlock
            if (objectEquals(mockThread.getResult(), true)) writeLock.unlock();
        }
    }
}
