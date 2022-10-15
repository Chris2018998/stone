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
 *       ThreadParker parker = ThreadParker.create(0,false);
 *       await(parker):
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      ThreadParker parker = ThreadParker.create(nanosTimeout,false);
 *      await(parker):
 *    }
 *    public final boolean awaitUntil(Date deadline) throws InterruptedException {
 *      ThreadParker parker = ThreadParker.create(deadline.getTime(),true);
 *      await(parker):
 *    }
 *   public final boolean await(long time, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      ThreadParker parker = ThreadParker.create(nanosTimeout,false);
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

//********************************************************************************************************************//
//                                           1:LockSupport.park                                                       //
//********************************************************************************************************************//
public class ThreadParker {
    //just test interrupted
    private static final ThreadInterruptedTest DEF_TEST = new ThreadInterruptedTest();
    //just test interrupted and clear interrupted flag
    private static final ThreadInterruptedTest CLEAR_TEST = new InterruptedTestAndClear();

    //park time,milliseconds(time point) or nanoSeconds(time value)
    protected long time;
    //timeout flag
    protected boolean timeout;
    //park block object
    protected Object blocker;
    //interrupted test
    protected ThreadInterruptedTest interruptedTest = DEF_TEST;

    //****************************************************************************************************************//
    //                                              constructors(2)                                                   //
    //****************************************************************************************************************//
    ThreadParker() {
    }

    ThreadParker(Object blocker) {
        this.blocker = blocker;
    }

    //****************************************************************************************************************//
    //                                           Factory  methods(begin)                                              //
    //****************************************************************************************************************//
    public static ThreadParker create(long time, boolean isMilliseconds) {
        if (time <= 0)
            return new ThreadParker();
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
    //                                           Factory  methods(end)                                                //
    //****************************************************************************************************************//

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

    //calculate next park time value,if timeout then return true
    public boolean calNextParkTime() {
        return false;
    }

    public boolean allowThrowInterruptedException() {
        return this.interruptedTest == DEF_TEST;
    }

    public void setAutoClearInterruptedFlag(boolean clearInd) {
        this.interruptedTest = clearInd ? CLEAR_TEST : DEF_TEST;
    }

    //blocker implementation sub class
    private static class ThreadObjectParker extends ThreadParker {
        ThreadObjectParker(Object blocker) {
            super(blocker);
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
            this.time = time;
            this.deadlineNs = System.nanoTime() + time;
        }

        public boolean calNextParkTime() {
            this.time = deadlineNs - System.nanoTime();
            return this.timeout = time <= 0;
        }

        public boolean park() {
            LockSupport.parkNanos(time);
            return interruptedTest.isInterrupted();
        }
    }

    private static class NanoSecondsObjectParker extends NanoSecondsParker {
        NanoSecondsObjectParker(long time, Object blocker) {
            super(time);
            this.blocker = blocker;
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
            this.time = time;
        }

        public boolean calNextParkTime() {
            return this.timeout = System.currentTimeMillis() >= time;
        }

        public boolean park() {
            LockSupport.parkUntil(time);
            return interruptedTest.isInterrupted();
        }
    }

    private static class MillisecondsObjectUtilParker extends MillisecondsUtilParker {
        MillisecondsObjectUtilParker(long time, Object blocker) {
            super(time);
            this.blocker = blocker;
        }

        public boolean park() {
            LockSupport.parkUntil(blocker, time);
            return interruptedTest.isInterrupted();
        }
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
}



