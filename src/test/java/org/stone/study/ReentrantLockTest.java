package org.stone.study;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class ReentrantLockTest {

    public static void main(String[] args) throws Exception {
        int count = 5000;
        long delayTime = TimeUnit.SECONDS.toNanos(2);
        long parkTime = System.nanoTime() + delayTime;
        Lock lock1 = new java.util.concurrent.locks.ReentrantLock();
        Lock lock2 = new org.stone.shine.util.concurrent.locks.ReentrantLock();

        String lock1Name = "JDK_Lock";
        String lock2Name = "Stone_Lock";
        LockThread[] threads = new LockThread[count];
        for (int i = 0; i < count; i++) {
            threads[i] = new LockThread(lock1Name, lock1, parkTime);
            threads[i].start();
        }
        long totTime = 0;
        for (int i = 0; i < count; i++) {
            threads[i].join();
            totTime = totTime + threads[i].tookTime;
        }

        parkTime = System.nanoTime() + delayTime;
        LockThread[] thread2s = new LockThread[count];
        for (int i = 0; i < count; i++) {
            thread2s[i] = new LockThread(lock2Name, lock2, parkTime);
            thread2s[i].start();
        }
        long totTime2 = 0;
        for (int i = 0; i < count; i++) {
            thread2s[i].join();
            totTime2 = totTime2 + thread2s[i].tookTime;
        }

        System.out.println("[" + lock1Name + "]Took time:" + totTime + "ms,avg=" + (totTime / count) + "ms");
        System.out.println("[" + lock2Name + "]Took time:" + totTime2 + "ms,avg=" + (totTime2 / count) + "ms");
    }

    private static class LockThread extends Thread {
        String name;
        long tookTime;
        private Lock lock;
        private long deadlineTime;

        LockThread(String name, Lock lock, long deadlineTime) {
            this.name = name;
            this.lock = lock;
            this.deadlineTime = deadlineTime;
        }

        public void run() {
            LockSupport.parkNanos(deadlineTime - System.nanoTime());
            long time1 = System.currentTimeMillis();
            lock.lock();

            lock.unlock();
            tookTime = System.currentTimeMillis() - time1;
        }
    }
}
