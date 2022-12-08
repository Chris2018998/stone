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

//********************************************************************************************************************//
//                          1:Implementation based on park method{@code LockSupport.park}                             //
//********************************************************************************************************************//
public class ThreadParkSupport implements Cloneable {
    private static final long spinForTimeoutThreshold = 1000L;
    protected long deadline;
    protected long parkTime = 1L;//a initialized value,calculated before park,if less than zero or equals zero means timeout
    protected Object blocker;
    protected boolean interrupted;

    //****************************************************************************************************************//
    //                                              constructors(2)                                                   //
    //****************************************************************************************************************//
    ThreadParkSupport() {
    }

    ThreadParkSupport(Object blocker) {
        this.blocker = blocker;
    }

    //****************************************************************************************************************//
    //                                           Factory  methods(2)                                                  //
    //****************************************************************************************************************//
    public static ThreadParkSupport create(long time, boolean isMilliseconds) {
        if (time <= 0) return new ThreadParkSupport();

        if (isMilliseconds) return new MillisecondsUtilParkSupport(time);

        return new NanoSecondsParkSupport(time);
    }

    public static ThreadParkSupport create(long time, boolean isMilliseconds, Object blocker) {
        if (time <= 0) return new ThreadObjectParkSupport(blocker);

        if (isMilliseconds) return new MillisecondsObjectUtilParkSupport(time, blocker);

        return new NanoSecondsObjectParkSupport(time, blocker);
    }

    //****************************************************************************************************************//
    //                                           get methods(4)                                                       //
    //****************************************************************************************************************//
    public final long getDeadline() {
        return deadline;
    }

    public final long getParkTime() {
        return parkTime;
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


    //Thead Park with block object for{@code LockSupport.park(blocker)}
    private static class ThreadObjectParkSupport extends ThreadParkSupport {
        ThreadObjectParkSupport(Object blocker) {
            super(blocker);
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
    //                     2: Implementation based on park method{@code LockSupport.parkNanos}                        //
    //****************************************************************************************************************//
    private static class NanoSecondsParkSupport extends ThreadParkSupport {
        NanoSecondsParkSupport(long nanoTime) {
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

    private static class NanoSecondsObjectParkSupport extends NanoSecondsParkSupport {
        NanoSecondsObjectParkSupport(long nanoTime, Object blocker) {
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
    //                        3: Implementation based on park method{@code LockSupport.parkUntil}                     //
    //****************************************************************************************************************//
    private static class MillisecondsUtilParkSupport extends ThreadParkSupport {
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
    }

    private static class MillisecondsObjectUtilParkSupport extends MillisecondsUtilParkSupport {
        MillisecondsObjectUtilParkSupport(long deadline, Object blocker) {
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



