/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.concurrent.cyclicBarrier;

import org.stone.shine.util.concurrent.CyclicBarrier2;

import java.util.concurrent.BrokenBarrierException;

/**
 * Cyclic Barrier Test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CyclicBarrierTest {

    public static void main(String[] args) throws Exception {
        int count = 5;
        CyclicBarrier2 barrier = new CyclicBarrier2(count, new TripAction());
        PassengerThread[] runnable = new PassengerThread[count];
        for (int i = 0; i < count; i++) {
            runnable[i] = new PassengerThread(barrier);
            runnable[i].start();
        }

        for (int i = 0; i < count; i++) {
            runnable[i].join();
            System.out.println("runnable[" + i + "] is end");
        }

        System.out.println("numberWaiting:" + barrier.getNumberWaiting());
    }

    static class TripAction extends Thread {
        public void run() {
            System.out.println("triped");
        }
    }

    static class PassengerThread extends Thread {
        private CyclicBarrier2 barrier2;

        public PassengerThread(CyclicBarrier2 barrier2) {
            this.barrier2 = barrier2;
        }

        public void run() {
            try {
                barrier2.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}
