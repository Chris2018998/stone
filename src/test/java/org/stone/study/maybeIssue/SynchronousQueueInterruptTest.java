package org.stone.study;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class SynchronousQueueInterruptTest {
    public static void main(String[] args) {
        SynchronousQueue queue = new SynchronousQueue(true);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            TakeThread taker = new TakeThread(queue);
            taker.start();

            long time = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            PutThread putter = new PutThread(queue, time);
            InterruptThread interrupt = new InterruptThread(taker, time);
            putter.start();
            interrupt.start();

            try {
                taker.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                putter.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                interrupt.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (taker.getTakeOutObject() != null && taker.isInterruptedInd()) {
                System.err.println("Found interruption in take thread in Loop[" + i + "]");
                break;
            }
        }
    }

    //take thread
    private static class TakeThread extends Thread {
        private Object takeOutObject;
        private boolean interruptedInd;
        private SynchronousQueue queue;

        TakeThread(SynchronousQueue queue) {
            this.queue = queue;
        }

        Object getTakeOutObject() {
            return takeOutObject;
        }

        boolean isInterruptedInd() {
            return interruptedInd;
        }

        public void run() {
            try {
                this.takeOutObject = queue.take();
                this.interruptedInd = this.isInterrupted();
            } catch (Exception e) {
            }
        }
    }

    //action thread to interrupt the take thread
    private static class InterruptThread extends Thread {
        private TakeThread takeThread;
        private long targetNanoSeconds;

        InterruptThread(TakeThread takeThread, long targetNanoSeconds) {
            this.takeThread = takeThread;
            this.targetNanoSeconds = targetNanoSeconds;
        }

        public void run() {
            LockSupport.parkNanos(targetNanoSeconds - System.nanoTime());
            takeThread.interrupt();
        }
    }

    //put thread
    private static class PutThread extends Thread {
        private SynchronousQueue queue;
        private long targetNanoSeconds;

        PutThread(SynchronousQueue queue, long targetNanoSeconds) {
            this.queue = queue;
            this.targetNanoSeconds = targetNanoSeconds;
        }

        public void run() {
            LockSupport.parkNanos(targetNanoSeconds - System.nanoTime());
            queue.offer("123");
        }
    }
}
