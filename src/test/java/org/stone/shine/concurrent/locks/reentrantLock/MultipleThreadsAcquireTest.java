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

/**
 * 1000 threads to use lock
 *
 * @author Chris Liao
 * @version 1.0
 */

public class MultipleThreadsAcquireTest {

    public static void main(String[] sgs) {
        int size = 1000;
        ReentrantLock lock = new ReentrantLock();
        LockTestThread[] testThread = new LockTestThread[size];
        for (int i = 0; i < size; i++)
            testThread[i] = new LockTestThread("Thread" + i, lock);
        for (int i = 0; i < size; i++)
            testThread[i].start();
    }

    private static class LockTestThread extends Thread {
        private ReentrantLock lock;

        public LockTestThread(String threadName, ReentrantLock lock) {
            this.lock = lock;
            this.setName(threadName);
        }

        public void run() {
            lock.lock();
            try {
                System.out.println(this.getName() + " is Working");
            } catch (Exception e) {
            } finally {
                lock.unlock();
            }
        }
    }
}
