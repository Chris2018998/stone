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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;


/**
 * 1000 threads to use lock
 *
 * @author Chris Liao
 * @version 1.0
 */

public class LockConcurrentTest extends TestCase {

    public void test() throws Exception {
        //1: create lock and acquire in main thread
        ReentrantLock lock = new ReentrantLock();
        long timePointToAWait = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);

        int size = 1000;
        LockTestThread[] testThread = new LockTestThread[size];
        for (int i = 0; i < size; i++)
            testThread[i] = new LockTestThread("Thread" + i, lock, timePointToAWait);
        for (int i = 0; i < size; i++)
            testThread[i].start();

        for (int i = 0; i < size; i++)
            testThread[i].join();

        for (int i = 0; i < size; i++)
            if (!testThread[i].acquired) TestUtil.assertError("multiple threads acquire Test failed");
    }

    //mock thread
    private static class LockTestThread extends Thread {
        private final ReentrantLock lock;
        private final long timePointToLock;
        private boolean acquired;

        public LockTestThread(String threadName, ReentrantLock lock, long timePointToLock) {
            this.lock = lock;
            this.setName(threadName);
            this.timePointToLock = timePointToLock;
        }

        public boolean isAcquired() {
            return acquired;
        }

        public void run() {
            LockSupport.parkNanos(timePointToLock - System.nanoTime());

            lock.lock();
            try {
                this.acquired = true;
            } catch (Exception e) {
            } finally {
                lock.unlock();
            }
        }
    }
}