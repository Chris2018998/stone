/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

import java.util.concurrent.locks.LockSupport;

import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Time parker class,supply three Implementation with park methods of {@link LockSupport} class
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
 *       ThreadSpinParker parker = ThreadSpinParker.create();
 *       await(parker):
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      ThreadSpinParker parker = ThreadSpinParker.create(nanosTimeout,false);
 *      await(parker):
 *    }
 *    public final boolean awaitUntil(Date deadline) throws InterruptedException {
 *      ThreadSpinParker parker = ThreadSpinParker.create(deadline.getParkTime(),true);
 *      await(parker):
 *    }
 *   public final boolean await(long parkTime, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      ThreadSpinParker parker = ThreadSpinParker.create(nanosTimeout,false);
 *      await(parker):
 *   }
 *   //spin control
 *   private void await(ThreadSpinParker  parker)throws InterruptedException {
 *      //spin source code.........
 *   }
 *  }//class end
 * }//code tag
 * </pre>
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ThreadSpinParker {
    final boolean allowInterrupted;
    protected long deadline;
    protected boolean timeout;
    protected boolean interrupted;
    Object blocker;
    long remainTime;

    //****************************************************************************************************************//
    //                                              constructors(1)                                                   //
    //****************************************************************************************************************//
    ThreadSpinParker(boolean allowInterrupted) {
        this.allowInterrupted = allowInterrupted;
    }

    //****************************************************************************************************************//
    //                                           get methods(4)                                                       //
    //****************************************************************************************************************//
    public final long getDeadline() {
        return deadline;
    }

    public final long getRemainTime() {
        return remainTime;
    }

    public final boolean isTimeout() {
        return timeout;
    }

    public final boolean isInterrupted() {
        return interrupted;
    }

    //****************************************************************************************************************//
    //                                           park methods(2) need be override                                     //
    //****************************************************************************************************************//
    public boolean computeParkTime() {
        return true;
    }

    public boolean parkUtilInterrupted() {
        LockSupport.park();
        return this.interrupted = Thread.interrupted() && allowInterrupted;
    }

    public void reset() {
        this.remainTime = 0;
        this.timeout = false;
    }

    public String toString() {
        return "Implementation with LockSupport.park()";
    }

    //****************************************************************************************************************//
    //                                           blocker park Implement                                               //
    //****************************************************************************************************************//
    static class ThreadBlockerParkSupport extends ThreadSpinParker {
        ThreadBlockerParkSupport(Object blocker, boolean allowInterrupted) {
            super(allowInterrupted);
            this.blocker = blocker;
        }

        public final boolean parkUtilInterrupted() {
            LockSupport.park(blocker);
            return this.interrupted = Thread.interrupted() && allowInterrupted;
        }

        public String toString() {
            return "Implementation with LockSupport.park(blocker)";
        }
    }

    //****************************************************************************************************************//
    //                                            NanoSeconds park Implement                                          //
    //****************************************************************************************************************//
    static class NanoSecondsParkSupport extends ThreadSpinParker {
        private final long nanoTime;

        NanoSecondsParkSupport(long nanoTime, boolean allowInterrupted) {
            super(allowInterrupted);
            this.nanoTime = nanoTime;
            this.deadline = System.nanoTime() + nanoTime;
        }

        public final void reset() {
            super.reset();
            this.deadline = System.nanoTime() + nanoTime;
        }

        public boolean computeParkTime() {
            this.remainTime = deadline - System.nanoTime();
            this.timeout = this.remainTime <= 0;
            return remainTime > spinForTimeoutThreshold;
        }

        public boolean parkUtilInterrupted() {
            LockSupport.parkNanos(remainTime);
            return this.interrupted = Thread.interrupted() && allowInterrupted;
        }

        public String toString() {
            return "Implementation with LockSupport.parkNanos(time)";
        }
    }

    //****************************************************************************************************************//
    //                                    NanoSeconds blocker park Implement                                          //
    //****************************************************************************************************************//
    static class NanoSecondsBlockerParkSupport extends NanoSecondsParkSupport {
        NanoSecondsBlockerParkSupport(long nanoTime, Object blocker, boolean allowInterrupted) {
            super(nanoTime, allowInterrupted);
            this.blocker = blocker;
        }

        public final boolean parkUtilInterrupted() {
            LockSupport.parkNanos(blocker, remainTime);
            return this.interrupted = Thread.interrupted() && allowInterrupted;
        }

        public String toString() {
            return "Implementation with LockSupport.parkNanos(blocker,time)";
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds parkUtil Implement                                          //
    //****************************************************************************************************************//
    static class MillisecondsUtilParkSupport extends ThreadSpinParker {
        MillisecondsUtilParkSupport(long deadline, boolean allowInterrupted) {
            super(allowInterrupted);
            this.deadline = deadline;
        }

        public boolean computeParkTime() {
            this.remainTime = deadline - System.currentTimeMillis();
            this.timeout = this.remainTime <= 0;
            return remainTime > spinForTimeoutThreshold;
        }

        public boolean parkUtilInterrupted() {
            LockSupport.parkUntil(deadline);
            return this.interrupted = Thread.interrupted() && allowInterrupted;
        }

        public String toString() {
            return "Implementation with LockSupport.parkUntil(deadline)";
        }

        public final void reset() {
            throw new IllegalArgumentException("can't support deadline reset");
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds blocker parkUtil Implement                                  //
    //****************************************************************************************************************//
    static class MillisecondsBlockerUtilParkSupport extends MillisecondsUtilParkSupport {
        MillisecondsBlockerUtilParkSupport(long deadline, Object blocker, boolean allowInterrupted) {
            super(deadline, allowInterrupted);
            this.blocker = blocker;
        }

        public final boolean parkUtilInterrupted() {
            LockSupport.parkUntil(blocker, deadline);
            return this.interrupted = Thread.interrupted() && allowInterrupted;
        }

        public String toString() {
            return "Implementation with LockSupport.parkUntil(blocker,deadline)";
        }
    }
}



