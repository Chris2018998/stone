package org.stone.shine.lock;

import org.stone.shine.synchronizer.locks.ReentrantLock;

public class LockTest {

    public static void main(String[] sgs) {
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try {
            System.out.println("Lock1");
            System.out.println("is Locked1" + lock.isLocked());
            System.out.println("is Locked1" + lock.isHeldByCurrentThread());
        } finally {
            lock.unlock();
        }

        System.out.println("is Locked2" + lock.isLocked());
        System.out.println("is Locked2" + lock.isHeldByCurrentThread());
        lock.lock();
        try {
            System.out.println("Lock2");
        } finally {
            lock.unlock();
        }
    }
}
