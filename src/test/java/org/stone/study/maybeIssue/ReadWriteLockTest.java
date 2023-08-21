package org.stone.study.maybeIssue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;
//import org.stone.shine.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockTest {
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public static void main(String[] args) {
        //1: main thread hold write and read lock
        writeLock.lock();//hold write lock
        readLock.lock();//hold read lock
        System.out.println("<INFO>:Main thread has hold write-lock and read-lock");

        //2: launch an another thread to acquire write-lock(it will being blocked in AQS wait chain)
        WriteAcquireThread writeThread = new WriteAcquireThread(writeLock);
        writeThread.start();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        if (writeThread.getState() != Thread.State.WAITING) throw new IllegalStateException();
        System.out.println("<INFO>:A thread has blocking in AQS wait queue(acquiring write lock)");

        //3:Block main thread util the write-lock thread to be WAITING state
        for (int i = 0; i < 5; i++) new ReadAcquireThread(readLock).start();
        //4:Block main thread util these read-lock threads to be WAITING state(Position of these read threads should be after the write thread)
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        System.out.println("<INFO>:Five threads have blocking in AQS wait queue(acquiring read lock)");

        //5: Release write lock in main thread(after unlock,read lock is still hold by main thread)
        writeLock.unlock();
        System.out.println("<INFO>:Main thread released the write-lock,who got the lock?");

        //6: Read thread go through with Share mode,will be propagate to others
        //here,I think those read-lock threads should be got read lock,but not effect
        //readLock.unlock();
    }

    //a mock thread to acquire Read-mode lock
    private static class ReadAcquireThread extends Thread {
        private ReentrantReadWriteLock.ReadLock readLock;

        ReadAcquireThread(ReentrantReadWriteLock.ReadLock readLock) {
            this.readLock = readLock;
        }

        public void run() {
            readLock.lock();
            System.out.println("Has gotten readLock");
            readLock.unlock();
        }
    }

    //a mock thread to acquire Write-mode lock
    private static class WriteAcquireThread extends Thread {
        private ReentrantReadWriteLock.WriteLock writeLock;

        WriteAcquireThread(ReentrantReadWriteLock.WriteLock writeLock) {
            this.writeLock = writeLock;
        }

        public void run() {
            writeLock.lock();
            System.out.println("Has gotten writeLock");
            writeLock.unlock();
        }
    }
}
