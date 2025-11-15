/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.study;

import org.stone.tools.extension.BeeSemaphore;
import org.stone.tools.extension.BeeSemaphorePermit;

import java.util.concurrent.TimeUnit;

/**
 * Semaphore Test Case
 *
 * @author chris liao
 */

public class BeeSemaphoreTest {

    public static void main(String[] args) throws Exception {
        int permitSize = 10;
        boolean fair = false;
        int threadSize = 5000;
        int loopSize = 1000;

        testBeeSemaphore(permitSize, fair, threadSize, loopSize);
    }

    private static void testBeeSemaphore(int permitSize, boolean fair, int threadSize, int loopSize) throws InterruptedException {
        BeeSemaphore semaphore = new BeeSemaphore(permitSize, fair);
        BeeSemaphoreThread[] threads = new BeeSemaphoreThread[threadSize];
        //final long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(100000L);
        final long deadlineNanos = TimeUnit.SECONDS.toNanos(5L);

        long startTime1 = System.currentTimeMillis();
        for (int i = 0; i < threadSize; i++)
            threads[i] = new BeeSemaphoreThread(semaphore, deadlineNanos, loopSize);

        for (int i = 0; i < threadSize; i++) threads[i].start();
        for (int i = 0; i < threadSize; i++) threads[i].join();
        System.out.println("Bee Time:" + (System.currentTimeMillis() - startTime1) + "ms'");
    }

    private static class BeeSemaphoreThread extends Thread {
        private final int loopSize;
        private final long deadlineNanos;
        private final BeeSemaphore semaphore;

        public BeeSemaphoreThread(BeeSemaphore semaphore, long deadlineNanos, int loopSize) {
            this.loopSize = loopSize;
            this.semaphore = semaphore;
            this.deadlineNanos = deadlineNanos;
        }

        public void run() {

            for (int i = 0; i < loopSize; i++) {
                try {
                    BeeSemaphorePermit permit = semaphore.tryAcquire(deadlineNanos, null);
                    if (permit != null) {
                        try {
                            //do something here
                        } finally {
                            semaphore.release(permit);
                        }
                    } else {
                        System.out.println("...beecp Timeout...");
                    }
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
    }
}



