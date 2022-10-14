/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.concurrent.locks.LockSupport;

/**
 * Time parker class,supply three implementation by park methods of LockSupport class
 * 1:LockSupport.park
 * 2:LockSupport.parkNanos
 * 3:LockSupport.parkUntil
 *
 * <p>
 * Class objective:reduce spin similar code (for example:some await methods in class:{@link java.util.concurrent.locks.AbstractQueuedSynchronizer})
 * Usage demo:
 * <pre>{@code
 * class ConditionX {
 *    public final void await() throws InterruptedException {
 *       ThreadParker parker = ThreadParkerFactory.create(0,false);
 *       await(parker):
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      ThreadParker parker = ThreadParkerFactory.create(nanosTimeout,false);
 *      await(parker):
 *    }
 *    public final boolean awaitUntil(Date deadline) throws InterruptedException {
 *      ThreadParker parker = ThreadParkerFactory.create(deadline.getTime(),true);
 *      await(parker):
 *    }
 *   public final boolean await(long time, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      ThreadParker parker = ThreadParkerFactory.create(nanosTimeout,false);
 *      await(parker):
 *   }
 *   //spin control
 *   private void await(ThreadParker  parker)throws InterruptedException {
 *      //spin source code.........
 *   }
 *  }//class end
 * }//code tag
 * </pre>
 *
 * @author Chris Liao
 * @version 1.0
 */

final class ThreadParkerFactory {
    //just test interrupted
    private static final ThreadInterruptedTest DEF_TEST = new ThreadInterruptedTest();
    //just test interrupted and clear interrupted flag
    private static final ThreadInterruptedTest CLEAR_TEST = new InterruptedTestAndClear();

    //****************************************************************************************************************//
    //                                           Factory  methods                                                     //
    //****************************************************************************************************************//
    public static ThreadParker create(long time, boolean isMilliseconds) {
        if (time <= 0)
            return new ThreadParker(0);
        else if (isMilliseconds)
            return new MillisecondsUtilParker(time);
        else
            return new NanoSecondsParker(time);
    }

    public static ThreadParker create(long time, boolean isMilliseconds, Object blocker) {
        if (time <= 0)
            return new ThreadObjectParker(blocker);
        else if (isMilliseconds)
            return new MillisecondsObjectUtilParker(time, blocker);
        else
            return new NanoSecondsObjectParker(time, blocker);
    }

    //****************************************************************************************************************//
    //                                           Thread Interrupted Test                                              //
    //****************************************************************************************************************//
    private static class ThreadInterruptedTest {
        public boolean isInterrupted() {
            return Thread.currentThread().isInterrupted();//just test
        }
    }

    private static class InterruptedTestAndClear extends ThreadInterruptedTest {
        public boolean isInterrupted() {
            return Thread.interrupted();//test and clear interrupted flag
        }
    }

    //****************************************************************************************************************//
    //                                           1:LockSupport.park                                                   //
    //****************************************************************************************************************//
    static class ThreadParker {
        //park time,milliseconds(time point) or nanoSeconds(time value)
        protected long time;
        //timeout flag
        protected boolean timeout;
        //park block object
        protected Object blocker;
        //interrupted test
        protected ThreadInterruptedTest interruptedTest = DEF_TEST;

        ThreadParker(long time) {
            this.time = time;
        }

        ThreadParker(long time, Object blocker) {
            this.time = time;
            this.blocker = blocker;
        }

        public void calNexTime() {
        }

        public synchronized boolean allowInterruptable() {
            return this.interruptedTest == DEF_TEST;
        }

        public synchronized void setAutoClearInterruptedInd(boolean clearInd) {
            this.interruptedTest = clearInd ? CLEAR_TEST : DEF_TEST;
        }

        //true:thread isInterrupted
        public boolean park() {
            LockSupport.park();
            return interruptedTest.isInterrupted();
        }

        public long getTime() {
            return time;
        }

        public boolean isTimeout() {
            return timeout;
        }
    }

    private static class ThreadObjectParker extends ThreadParker {
        ThreadObjectParker(Object blocker) {
            super(0, blocker);
        }

        public boolean park() {
            LockSupport.park(blocker);
            return interruptedTest.isInterrupted();
        }
    }

    //****************************************************************************************************************//
    //                                           2: LockSupport.parkNanos                                             //
    //****************************************************************************************************************//
    private static class NanoSecondsParker extends ThreadParker {
        private long deadlineNs;

        NanoSecondsParker(long time) {
            super(time);
            this.deadlineNs = System.nanoTime() + time;
        }

        NanoSecondsParker(long time, Object blocker) {
            super(time, blocker);
            this.deadlineNs = System.nanoTime() + time;
        }

        public void calNexTime() {
            this.time = deadlineNs - System.nanoTime();
            this.timeout = time <= 0;
        }

        public boolean park() {
            LockSupport.parkNanos(time);
            return interruptedTest.isInterrupted();
        }
    }

    private static class NanoSecondsObjectParker extends NanoSecondsParker {
        NanoSecondsObjectParker(long time, Object blocker) {
            super(time, blocker);
        }

        public boolean park() {
            LockSupport.parkNanos(blocker, time);
            return interruptedTest.isInterrupted();
        }
    }

    //****************************************************************************************************************//
    //                                           3: LockSupport.parkUntil                                              //
    //****************************************************************************************************************//
    private static class MillisecondsUtilParker extends ThreadParker {
        MillisecondsUtilParker(long time) {
            super(time);
        }

        MillisecondsUtilParker(long time, Object blocker) {
            super(time, blocker);
        }

        public void calNexTime() {
            this.timeout = System.currentTimeMillis() >= time;
        }

        public boolean park() {
            LockSupport.parkUntil(time);
            return interruptedTest.isInterrupted();
        }
    }

    private static class MillisecondsObjectUtilParker extends MillisecondsUtilParker {
        MillisecondsObjectUtilParker(long time, Object blocker) {
            super(time, blocker);
        }

        public boolean park() {
            LockSupport.parkUntil(blocker, time);
            return interruptedTest.isInterrupted();
        }
    }
}


