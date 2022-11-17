package org.stone.shine.lock;

import org.stone.shine.synchronizer.locks.ReentrantLock;

public class LockTest {

    public static void main(String[] sgs) {
        int size = 10;
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
            } finally {
                lock.unlock();
            }
        }
    }
}
