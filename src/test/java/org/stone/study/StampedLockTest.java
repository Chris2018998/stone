package org.stone.study;

import org.stone.shine.util.concurrent.locks.StampedLock;

public class StampedLockTest {

    public static void main(String[] args) throws Exception {
        int size = 10000;
        WriteThread[] threads = new WriteThread[size];
        StampedLock lock = new StampedLock();
        for (int i = 0; i < size; i++) {
            threads[i] = new WriteThread(i, lock);
            threads[i].start();
        }
        long total = 0;
        for (int i = 0; i < size; i++) {
            threads[i].join();
            total += threads[i].tookTime;
        }
        System.out.println("Took time:" + (total) + "ms,avg time:" + total / size);

//        stampWriteLock();
//        stampReadLock();
    }

    private static void stampWriteLock() {
        StampedLock lock = new StampedLock();
        long stamp = lock.writeLock();
        System.out.println("writeLock: " + stamp);

        //lock.tryConvertToOptimisticRead();
        long writeStamp = lock.tryConvertToWriteLock(stamp);
        System.out.println("tryConvertToWriteLock: " + writeStamp);
        long readStamp = lock.tryConvertToReadLock(stamp);
        System.out.println("tryConvertToReadLock: " + readStamp);
    }

    private static void stampReadLock() {
        StampedLock lock = new StampedLock();
        long stamp = lock.readLock();
        System.out.println("readLock: " + stamp);
        long readStamp = lock.tryConvertToReadLock(stamp);
        System.out.println("tryConvertToReadLock: " + readStamp);
        long writeStamp = lock.tryConvertToWriteLock(stamp);
        System.out.println("tryConvertToWriteLock: " + writeStamp);
    }

    static class WriteThread extends Thread {
        int index;
        StampedLock lock;
        long tookTime;

        public WriteThread(int index, StampedLock lock) {
            this.index = index;
            this.lock = lock;
        }

        public void run() {
            long time1 = System.currentTimeMillis();
            long stamp = lock.writeLock();
            try {
            } finally {
                if (stamp > 0) lock.unlockWrite(stamp);
                tookTime = System.currentTimeMillis() - time1;
            }
        }
    }
}
