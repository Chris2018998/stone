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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;
import static java.util.concurrent.locks.LockSupport.parkUntil;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Time parker class,supply three Implementation with park methods of {@link @java.util.concurrent.locks.LockSupport} class
 * 1:park
 * 2:parkNanos
 * 3:parkUntil
 *
 * <p>
 * Class objective:reduce spin similar code (for example:some await methods in class:{@link @java.util.concurrent.locks.AbstractQueuedSynchronizer})
 * Usage demo:
 * <pre>{@code
 * class ConditionX {
 *    public final void await() throws InterruptedException {
 *       ThreadParkSupport parker = ThreadParkSupport.create();
 *       await(parker);
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      ThreadParkSupport parker = ThreadParkSupport.create(nanosTimeout,false);
 *      await(parker);
 *    }
 *    public final boolean awaitUntil(Date deadlineNanos) throws InterruptedException {
 *      ThreadParkSupport parker = ThreadParkSupport.create(deadlineNanos.getParkNanos(),true);
 *      await(parker);
 *    }
 *   public final boolean await(long parkNanos, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      ThreadParkSupport parker = ThreadParkSupport.create(nanosTimeout,false);
 *      await(parker);
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
    boolean timePark;
    long parkNanos;//compute before parking,if less than zero or equals zero means timeout
    long deadlineNanos;//nanoseconds
    Object blockObject;
    boolean interrupted;

    ThreadParkSupport() {
        this.timePark = false;
        this.parkNanos = spinForTimeoutThreshold + 1;//dummy value for park method,that's means not timeout
    }

    public final boolean isTimePark() {
        return timePark;
    }

    public final long getParkNanos() {
        return parkNanos;
    }

    public final long getDeadlineNanos() {
        return deadlineNanos;
    }

    public final boolean isTimeout() {
        return parkNanos <= 0;
    }

    public final boolean isInterrupted() {
        return interrupted;
    }

    public long computeParkNanos() {
        return parkNanos;
    }

    public boolean park() {
        LockSupport.park();
        return this.interrupted = Thread.interrupted();
    }

    public void reset() {
        this.interrupted = false;
    }

    public String toString() {
        return "Implementation with method 'park()'";
    }

    //****************************************************************************************************************//
    //                                           object park Implement                                                //
    //****************************************************************************************************************//
    static class ThreadParkSupport2 extends ThreadParkSupport {
        ThreadParkSupport2(Object blocker) {
            this.blockObject = blocker;
        }

        public final boolean park() {
            LockSupport.park(blockObject);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'park(blockObject)'";
        }
    }

    //****************************************************************************************************************//
    //                                            NanoSeconds park Implement                                          //
    //****************************************************************************************************************//
    static class NanoSecondsParkSupport extends ThreadParkSupport {
        private final long nanoTime;

        NanoSecondsParkSupport(long nanoTime) {
            this.timePark = true;
            this.nanoTime = nanoTime;
            this.deadlineNanos = System.nanoTime() + nanoTime;
        }

        public final void reset() {
            super.reset();
            this.deadlineNanos = System.nanoTime() + nanoTime;
        }

        public long computeParkNanos() {
            return this.parkNanos = deadlineNanos - System.nanoTime();
        }

        public boolean park() {
            parkNanos(parkNanos);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'parkNanos(time)'";
        }
    }

    //****************************************************************************************************************//
    //                                    NanoSeconds blockObject park Implement                                      //
    //****************************************************************************************************************//
    static class NanoSecondsParkSupport2 extends NanoSecondsParkSupport {
        NanoSecondsParkSupport2(long nanoTime, Object blocker) {
            super(nanoTime);
            this.blockObject = blocker;
        }

        public final boolean park() {
            parkNanos(blockObject, parkNanos);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'parkNanos(time,blockObject)'";
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds park Implement                                              //
    //****************************************************************************************************************//
    static class UtilMillsParkSupport1 extends ThreadParkSupport {
        final long deadlineMillis;//nanoseconds or milliseconds

        UtilMillsParkSupport1(long deadline) {
            this.timePark = true;
            this.deadlineMillis = deadline;
            this.deadlineNanos = MILLISECONDS.toNanos(deadlineMillis);//nanoseconds
        }

        public final long computeParkNanos() {
            return this.parkNanos = MILLISECONDS.toNanos(deadlineMillis - System.currentTimeMillis());
        }

        public boolean park() {
            parkUntil(deadlineMillis);
            return interrupted = Thread.interrupted();
        }


        public String toString() {
            return "Implementation with method 'parkUntil(milliseconds)'";
        }

        public final void reset() {
            throw new IllegalArgumentException("can't support absolute time reset");
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds park util Implement                                         //
    //****************************************************************************************************************//
    static class UtilMillsParkSupport2 extends UtilMillsParkSupport1 {
        UtilMillsParkSupport2(long deadline, Object blocker) {
            super(deadline);
            this.blockObject = blocker;
        }

        public boolean park() {
            parkUntil(blockObject, deadlineMillis);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'parkUntil(milliseconds,blockObject)'";
        }
    }
}



