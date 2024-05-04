/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.concurrent.cyclicBarrier;

import org.stone.shine.util.concurrent.CyclicBarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Cyclic Barrier Test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConcurrentAwaitTest {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100; i++) {
            doTest(i);
        }
    }

    private static void doTest(int j) throws Exception {
        int count = 10;
        long time = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        CyclicBarrier barrier = new CyclicBarrier(count, new TripAction());
        PassengerThread[] runnable = new PassengerThread[count];
        for (int i = 0; i < count; i++) {
            runnable[i] = new PassengerThread(barrier, time);
            runnable[i].start();
        }

        for (int i = 0; i < count; i++) {
            runnable[i].join();
            //System.out.println("runnable[" + i + "] is end");
        }

        System.out.println("Loop[" + j + "]numberWaiting:" + barrier.getNumberWaiting());
    }


    static class TripAction extends Thread {
        public void run() {
            System.out.println("triped");
        }
    }

    static class PassengerThread extends Thread {
        private final long time;
        private final CyclicBarrier barrier2;

        PassengerThread(CyclicBarrier barrier2, long time) {
            this.barrier2 = barrier2;
            this.time = time;
        }

        public void run() {
            try {
                LockSupport.parkNanos(time - System.nanoTime());
                barrier2.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}
