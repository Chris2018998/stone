package org.stone.study.maybeIssue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.SynchronousQueue;

public class InterruptedStatusNotClearTest {
    public static void main(String[] args) {
        SynchronousQueue queue = new SynchronousQueue(true);
        long delayTime = TimeUnit.SECONDS.toNanos(1);
        int loopIndex = -1;

        int count = 100;//Integer.MAX_VALUE
        for (int i = 0; i < count; i++) {
            System.out.println("Loop[" + i + "]");
            CountDownLatch latch = new CountDownLatch(3);
            TakeThread takeThread = new TakeThread(queue, latch);
            takeThread.start();

            long time = System.nanoTime() + delayTime;
            PutThread putThread = new PutThread(queue, time, latch);
            InterruptThread interrupt = new InterruptThread(takeThread, time, latch);
            putThread.start();
            interrupt.start();

            try {
                latch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (takeThread.getTakeOutObject() != null && takeThread.isInterruptedInd()) {
                loopIndex = i;
                break;
            }

            latch = null;
            takeThread = null;
            putThread = null;
            interrupt = null;
        }

        if (loopIndex == -1) {
            System.err.println("消费者线程上未发现中断标记");
        } else {
            System.err.println("第[" + loopIndex + "]轮次，发现消费者线程上存在未清除的中断标记");
        }
    }

    //take thread
    private static class TakeThread extends Thread {
        private Object takeOutObject;
        private boolean interruptedInd;
        private SynchronousQueue queue;
        private CountDownLatch latch;

        TakeThread(SynchronousQueue queue, CountDownLatch latch) {
            this.queue = queue;
            this.latch = latch;
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
            latch.countDown();
        }
    }

    //action thread to interrupt the take thread
    private static class InterruptThread extends Thread {
        private TakeThread takeThread;
        private long targetNanoSeconds;
        private CountDownLatch latch;

        InterruptThread(TakeThread takeThread, long targetNanoSeconds, CountDownLatch latch) {
            this.takeThread = takeThread;
            this.targetNanoSeconds = targetNanoSeconds;
            this.latch = latch;
        }

        public void run() {
            LockSupport.parkNanos(targetNanoSeconds - System.nanoTime());
            takeThread.interrupt();
            latch.countDown();
        }
    }

    //put thread
    private static class PutThread extends Thread {
        private SynchronousQueue queue;
        private long targetNanoSeconds;
        private CountDownLatch latch;

        PutThread(SynchronousQueue queue, long targetNanoSeconds, CountDownLatch latch) {
            this.queue = queue;
            this.targetNanoSeconds = targetNanoSeconds;
            this.latch = latch;
        }

        public void run() {
            LockSupport.parkNanos(targetNanoSeconds - System.nanoTime());
            queue.offer("123");
            latch.countDown();
        }
    }
}
