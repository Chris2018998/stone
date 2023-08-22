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

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.shine.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.concurrent.locks.Lock;


/**
 * 1000 threads to use writeLock
 *
 * @author Chris Liao
 * @version 1.0
 */

public class LockMultipleAcquireTest extends TestCase {
    public static void main(String[] args) throws Throwable {
        LockMultipleAcquireTest tester = new LockMultipleAcquireTest();
        tester.setUp();
        tester.test();
    }

    public void test() throws Exception {
        //1: create writeLock and acquire in main thread
        Lock lock = new ReentrantReadWriteLock().writeLock();

        int size = 1000;
        LockTestThread[] testThread = new LockTestThread[size];
        for (int i = 0; i < size; i++) {
            testThread[i] = new LockTestThread("Thread" + i, lock);
            testThread[i].start();
        }

        for (int i = 0; i < size; i++)
            testThread[i].join();

        for (int i = 0; i < size; i++)
            if (!testThread[i].acquired) TestUtil.assertError("multiple threads acquire Test failed");
    }

    //mock thread
    private static class LockTestThread extends Thread {
        private boolean acquired;
        private Lock lock;

        public LockTestThread(String threadName, Lock lock) {
            this.lock = lock;
            this.setName(threadName);
        }

        public boolean isAcquired() {
            return acquired;
        }

        public void run() {
            lock.lock();
            try {
                this.acquired = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }
}

