package org.stone.study;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class InterruptStatusNotClearTest {

    public static void main(String[] args) {
        SynchronousQueue queue = new SynchronousQueue(true);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            TakeThread taker = new TakeThread(queue);
            taker.start();

            long time = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            PutThread putter = new PutThread(queue, time);
            InterruptThread interrupt = new InterruptThread(taker, time);
            putter.start();
            interrupt.start();

            try {
                taker.join();
            } catch (Exception e) {
            }
            try {
                putter.join();
            } catch (Exception e) {
            }
            try {
                interrupt.join();
            } catch (Exception e) {
            }

            if (taker.interruptStatusNotClear) {
                System.err.println("第（"+(i+1)+"）次发现中断标记未清除");
                break;
            }
        }
    }

    //take thread
    private static class TakeThread extends Thread {
        boolean interruptStatusNotClear;
        private SynchronousQueue queue;

        TakeThread(SynchronousQueue queue) {
            this.queue = queue;
        }

        public void run() {
            try {
                Object a = queue.take();
                if (a != null && this.isInterrupted())
                    this.interruptStatusNotClear = true;
            } catch (Exception e) {
            }
        }
    }

    //Interrupt thread
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
