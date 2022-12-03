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
 * ReadLockToLockWriteLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class WriteLockReentrantTest extends ReentrantReadWriteLockTestCase {

    public void test() throws Exception {
        try {
            writeLock.lock();
            writeLock.lock();

            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, TestUtil.invokeMethod(writeLock, "isLocked"));
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", 2, TestUtil.invokeMethod(writeLock, "getHoldCount"));
        } finally {
            writeLock.unlock();
            writeLock.unlock();
        }
    }
}
