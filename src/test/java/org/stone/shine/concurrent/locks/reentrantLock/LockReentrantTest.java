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
import org.stone.shine.synchronizer.locks.ReentrantLock;

/**
 * ReentrantLock test case
 *
 * @author Chris Liao
 * @version 1.0
 */
public class LockReentrantTest extends TestCase {

    public void test() throws Exception {
        //1: create lock and acquire in main thread
        ReentrantLock lock = new ReentrantLock();

        try {
            lock.lock();
            lock.lock();

            TestUtil.assertError("test failed,expect value:%s,actual value:%s", true, lock.isLocked());
            TestUtil.assertError("test failed,expect value:%s,actual value:%s", 2, lock.getHoldCount());
        } finally {
            lock.unlock();
            lock.unlock();
        }
    }
}
