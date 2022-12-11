/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer;

import java.util.concurrent.locks.LockSupport;

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
 *       ThreadParkSupport parker = ThreadParkSupport.create();
 *       await(parker):
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      ThreadParkSupport parker = ThreadParkSupport.create(nanosTimeout,false);
 *      await(parker):
 *    }
 *    public final boolean awaitUntil(Date deadline) throws InterruptedException {
 *      ThreadParkSupport parker = ThreadParkSupport.create(deadline.getParkTime(),true);
 *      await(parker):
 *    }
 *   public final boolean await(long parkTime, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      ThreadParkSupport parker = ThreadParkSupport.create(nanosTimeout,false);
 *      await(parker):
 *   }
 *   //spin control
 *   private void await(ThreadParkSupport  parker)throws InterruptedException {
 *      //spin source code.........
 *   }
 *  }//class end
 * }//code tag
 * </pre>
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ThreadParkSupport {
    private static final long spinForTimeoutThreshold = 1000L;
    protected long deadline;
    protected long parkTime = 1L;//a initialized value,calculated before park,if less than zero or equals zero means timeout
    protected Object blocker;
    protected boolean interrupted;

    //****************************************************************************************************************//
    //                                              constructors(1)                                                   //
    //****************************************************************************************************************//
    ThreadParkSupport() {
    }

    //****************************************************************************************************************//
    //                                           get methods(4)                                                       //
    //****************************************************************************************************************//
    public final long getParkTime() {
        return parkTime;
    }

    public final long getDeadline() {
        return deadline;
    }

    public final boolean isTimeout() {
        return parkTime <= 0;
    }

    public final boolean isInterrupted() {
        return interrupted;
    }

    public String toString() {
        return "Implementation with LockSupport.park()";
    }

    public void reset() {
        this.parkTime = 1L;
        this.interrupted = false;
    }

    //****************************************************************************************************************//
    //                                           park methods(2) need be override                                     //
    //****************************************************************************************************************//
    //calculate park time for{@code LockSupport.park},true that time value is not timeout
    public boolean calculateParkTime() {
        return true;
    }

    public boolean park() {
        LockSupport.park();
        return interrupted = Thread.interrupted();
    }

    //****************************************************************************************************************//
    //                                           blocker park Implement                                               //
    //****************************************************************************************************************//
    static class ThreadBlockerParkSupport extends ThreadParkSupport {
        ThreadBlockerParkSupport(Object blocker) {
            this.blocker = blocker;
        }

        public final boolean park() {
            LockSupport.park(blocker);
            return interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with LockSupport.park(blocker)";
        }
    }

    //****************************************************************************************************************//
    //                                            NanoSeconds park Implement                                          //
    //****************************************************************************************************************//
    static class NanoSecondsParkSupport extends ThreadParkSupport {
        private final long nanoTime;

        NanoSecondsParkSupport(long nanoTime) {
            this.nanoTime = nanoTime;
            this.deadline = System.nanoTime() + nanoTime;
        }

        public void reset() {
            super.reset();
            this.deadline = System.nanoTime() + nanoTime;
        }

        //true means that time point before deadline
        public final boolean calculateParkTime() {
            this.parkTime = deadline - System.nanoTime();
            return parkTime > 0;
        }

        public boolean park() {
            if (parkTime > spinForTimeoutThreshold) {
                LockSupport.parkNanos(parkTime);
                this.interrupted = Thread.interrupted();
            }
            return interrupted;
        }

        public String toString() {
            return "Implementation with LockSupport.parkNanos(time)";
        }
    }

    //****************************************************************************************************************//
    //                                    NanoSeconds blocker park Implement                                          //
    //****************************************************************************************************************//
    static class NanoSecondsBlockerParkSupport extends NanoSecondsParkSupport {
        NanoSecondsBlockerParkSupport(long nanoTime, Object blocker) {
            super(nanoTime);
            this.blocker = blocker;
        }

        public final boolean park() {
            if (parkTime > spinForTimeoutThreshold) {
                LockSupport.parkNanos(parkTime);
                return interrupted = Thread.interrupted();
            } else {
                return interrupted;
            }
        }

        public String toString() {
            return "Implementation with LockSupport.parkNanos(time,blocker)";
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds parkUtil Implement                                          //
    //****************************************************************************************************************//
    static class MillisecondsUtilParkSupport extends ThreadParkSupport {
        MillisecondsUtilParkSupport(long deadline) {
            this.deadline = deadline;
        }

        public final boolean calculateParkTime() {
            this.parkTime = deadline - System.currentTimeMillis();
            return parkTime > 0;
        }

        public boolean park() {
            LockSupport.parkUntil(deadline);
            return interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with LockSupport.parkUntil(deadline)";
        }

        public void reset() {
            throw new IllegalArgumentException("can't support deadline reset");
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds blocker parkUtil Implement                                  //
    //****************************************************************************************************************//
    static class MillisecondsBlockerUtilParkSupport extends MillisecondsUtilParkSupport {
        MillisecondsBlockerUtilParkSupport(long deadline, Object blocker) {
            super(deadline);
            this.blocker = blocker;
        }

        public final boolean park() {
            LockSupport.parkUntil(blocker, deadline);
            return interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with LockSupport.parkUntil(deadline,blocker)";
        }
    }
}



