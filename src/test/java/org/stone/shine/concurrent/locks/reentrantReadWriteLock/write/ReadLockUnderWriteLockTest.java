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
import org.stone.shine.concurrent.locks.reentrantReadWriteLock.ReentrantReadWriteLockTestCase;

/**
 * Read Lock under WriteLock
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReadLockUnderWriteLockTest extends ReentrantReadWriteLockTestCase {
    public void test() throws Exception {

        writeLock.lock();
        readLock.lock();
        TestUtil.assertError("WriteLock test fail,expect value:%s,actual value:%s", 1, TestUtil.invokeMethod(readLock, "getHoldCount"));
        readLock.lock();
        TestUtil.assertError("WriteLock test fail,expect value:%s,actual value:%s", 2, TestUtil.invokeMethod(readLock, "getHoldCount"));

        writeLock.unlock();

        readLock.unlock();
        TestUtil.assertError("WriteLock test fail,expect value:%s,actual value:%s", 1, TestUtil.invokeMethod(readLock, "getHoldCount"));
        readLock.unlock();
        TestUtil.assertError("WriteLock test fail,expect value:%s,actual value:%s", 0, TestUtil.invokeMethod(readLock, "getHoldCount"));

    }
}
