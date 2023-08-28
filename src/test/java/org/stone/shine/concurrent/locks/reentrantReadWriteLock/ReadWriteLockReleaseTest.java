package org.stone.shine.concurrent.locks.reentrantReadWriteLock;

import org.stone.base.TestUtil;
import org.stone.shine.util.concurrent.locks.ReentrantReadWriteLock;

//import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockReleaseTest {
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public static void main(String[] args) throws Exception {
        //1: main thread hold write and read lock
        writeLock.lock();//hold write lock
        readLock.lock();//hold read lock
        System.out.println("<INFO>:Main thread has hold write-lock and read-lock");

        //2: launch an another thread to acquire write-lock(it will being blocked in AQS wait chain)
        WriteAcquireThread writeThread = new WriteAcquireThread();
        writeThread.start();
        if (!TestUtil.joinUtilWaiting(writeThread)) throw new java.lang.IllegalStateException();
        System.out.println("<INFO>:A thread has blocking in AQS wait queue(acquiring write lock)");

        //3:Block main thread util the write-lock thread to be WAITING state
        ReadAcquireThread readThead = new ReadAcquireThread();
        readThead.start();
        if (!TestUtil.joinUtilWaiting(readThead)) throw new java.lang.IllegalStateException();
        //4:Block main thread util these read-lock threads to be WAITING state(Position of these read threads should be after the write thread)
        System.out.println("<INFO>:one thread have blocking in AQS wait queue(acquiring read lock)");

        //5: Release write lock in main thread(after unlock,read lock is still hold by main thread)
        writeLock.unlock();
        System.out.println("<INFO>:Main thread released the write-lock,who got the lock?");

        //6: Read thread go through with Share mode,will be propagate to others
        //here,I think those read-lock threads should be got read lock,but not effect
        readLock.unlock();

        writeThread.join();
        readThead.join();
        if (readThead.time < writeThread.time)
            throw new java.lang.IllegalStateException("Read thread has got read-lock before Write thread");
    }

    //a mock thread to acquire Read-mode lock
    private static class ReadAcquireThread extends Thread {
        private long time;

        public void run() {
            readLock.lock();
            this.time = System.nanoTime();
            System.out.println("Has gotten readLock");
            System.out.println("Read count:" + lock.getReadLockCount());
            System.out.println("Concurrent hold Read count:" + lock.getReadHoldCount());
            readLock.unlock();
        }
    }

    //a mock thread to acquire Write-mode lock
    private static class WriteAcquireThread extends Thread {
        private long time;

        public void run() {
            writeLock.lock();
            this.time = System.nanoTime();
            System.out.println("Has gotten writeLock");
            writeLock.unlock();
        }
    }
}
