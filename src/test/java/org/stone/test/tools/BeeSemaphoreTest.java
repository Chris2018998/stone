/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.tools;

import org.stone.tools.extension.BeeSemaphore;
import org.stone.tools.extension.BeeSemaphorePermit;
import org.stone.tools.extension.BeeTransferQueueNode;

import java.util.concurrent.TimeUnit;

/**
 * Semaphore Test Case
 *
 * @author chris liao
 */

public class BeeSemaphoreTest {

    public static void main(String[] args) throws Exception {
        int threadSize = 10000;
        BeeSemaphore semaphore = new BeeSemaphore(10);
        BeeSemaphoreThread[] threads = new BeeSemaphoreThread[threadSize];
        final long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);

        for (int i = 0; i < threadSize; i++)
            threads[i] = new BeeSemaphoreThread(semaphore, deadlineNanos);

        for (int i = 0; i < threadSize; i++) threads[i].start();
        for (int i = 0; i < threadSize; i++) threads[i].join();
    }

    private static class BeeSemaphoreThread extends Thread {
        private final long deadlineNanos;
        private final BeeSemaphore semaphore;

        public BeeSemaphoreThread(BeeSemaphore semaphore, long deadlineNanos) {
            this.semaphore = semaphore;
            this.deadlineNanos = deadlineNanos;
        }

        public void run() {
            try {
                BeeSemaphorePermit permit = semaphore.tryAcquire(deadlineNanos, new BeeTransferQueueNode(this));
                if (permit != null) {
                    try {
                        //do something here
                    } finally {
                        semaphore.release(permit);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
    }
}



