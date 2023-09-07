package org.stone.study;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ReentrantLockTest {

    public static void main(String[] args) throws Exception {
        int count = 5000;
        long parkTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
        LockThread[] threads = new LockThread[count];
        for (int i = 0; i < count; i++) {
            threads[i] = new LockThread(lock, parkTime);
            threads[i].start();
        }
        long totTime = 0;
        for (int i = 0; i < count; i++) {
            threads[i].join();
            totTime = totTime + threads[i].tookTime;
        }

        parkTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        org.stone.shine.util.concurrent.locks.ReentrantLock lock2 = new org.stone.shine.util.concurrent.locks.ReentrantLock();
        LockThread2[] thread2s = new LockThread2[count];
        for (int i = 0; i < count; i++) {
            thread2s[i] = new LockThread2(lock2, parkTime);
            thread2s[i].start();
        }
        long totTime2 = 0;
        for (int i = 0; i < count; i++) {
            thread2s[i].join();
            totTime2 = totTime2 + thread2s[i].tookTime;
        }

        System.out.println("JDK Lock Took time:" + totTime + "ms,avg=" + (totTime / count) + "ms");
        System.out.println("JDK2 Lock Took time:" + totTime2 + "ms,avg=" + (totTime2 / count) + "ms");
    }

    private static class LockThread extends Thread {
        long tookTime;
        private java.util.concurrent.locks.ReentrantLock lock;
        private long parkTime;

        LockThread(java.util.concurrent.locks.ReentrantLock lock, long parkTime) {
            this.lock = lock;
            this.parkTime = parkTime;
        }

        public void run() {
            LockSupport.parkNanos(parkTime - System.nanoTime());
            long time1 = System.currentTimeMillis();
            lock.lock();
            try {
            } finally {
                lock.unlock();
                tookTime = System.currentTimeMillis() - time1;
            }
        }
    }

    private static class LockThread2 extends Thread {
        long tookTime;
        private org.stone.shine.util.concurrent.locks.ReentrantLock lock;
        private long parkTime;

        LockThread2(org.stone.shine.util.concurrent.locks.ReentrantLock lock, long parkTime) {
            this.lock = lock;
            this.parkTime = parkTime;
        }

        public void run() {
            LockSupport.parkNanos(parkTime - System.nanoTime());
            long time1 = System.currentTimeMillis();

            lock.lock();
            try {
            } finally {
                lock.unlock();
                tookTime = System.currentTimeMillis() - time1;
            }
        }
    }
}
