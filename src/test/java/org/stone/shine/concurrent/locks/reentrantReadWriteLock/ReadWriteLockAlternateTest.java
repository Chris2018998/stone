package org.stone.shine.concurrent.locks.reentrantReadWriteLock;

import org.stone.shine.concurrent.ConcurrentTimeUtil;
import org.stone.shine.util.concurrent.locks.ReentrantReadWriteLock;

//import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockAlternateTest {
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public static void main(String[] args) {
        writeLock.lock();//主线程获写锁
        int i = 1;
        Thread thread1 = new ReadThread("读线程[" + (i++) + "]");
        thread1.start();
        if (!ConcurrentTimeUtil.isInWaiting(thread1, 5L)) throw new java.lang.IllegalStateException();

        Thread thread2 = new WriteThread("写线程[" + (i++) + "]");
        thread2.start();
        if (!ConcurrentTimeUtil.isInWaiting(thread2, 5L)) throw new java.lang.IllegalStateException();

        Thread thread13 = new ReadThread("读线程[" + (i++) + "]");
        thread13.start();
        if (!ConcurrentTimeUtil.isInWaiting(thread13, 5L)) throw new java.lang.IllegalStateException();

        Thread thread4 = new WriteThread("写线程[" + (i++) + "]");
        thread4.start();
        if (!ConcurrentTimeUtil.isInWaiting(thread4, 5L)) throw new java.lang.IllegalStateException();

        writeLock.unlock();
    }

    private static class ReadThread extends Thread {
        private String name;

        ReadThread(String name) {
            this.name = name;
        }

        public void run() {
            readLock.lock();
            System.out.println(name + "获得读锁");
            readLock.unlock();
        }
    }

    private static class WriteThread extends Thread {
        private String name;

        WriteThread(String name) {
            this.name = name;
        }

        public void run() {
            writeLock.lock();
            System.out.println(name + "获得写锁");
            writeLock.unlock();
        }
    }
}
