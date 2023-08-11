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

/**
 * Time parker class,supply three Implementation with park methods of {@link java.util.concurrent.locks.LockSupport} class
 * 1:park
 * 2:parkNanos
 * 3:parkUntil
 *
 * <p>
 * Class objective:reduce spin similar code (for example:some await methods in class:{@link java.util.concurrent.locks.AbstractQueuedSynchronizer})
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
    Object blockObject;
    long deadlineTime;//time point(nanoseconds or milliseconds),value greater than 0,current is a time park
    long parkNanos;//value of last park time

    boolean hasTimeout;
    boolean interrupted;

    ThreadParkSupport() {
    }

    public final long getDeadlineTime() {
        return deadlineTime;
    }

    public final long getLastParkNanos() {
        return parkNanos;
    }

    public final boolean isTimeout() {
        return hasTimeout;
    }

    public final boolean isInterrupted() {
        return interrupted;
    }

    public void reset() {
        this.hasTimeout = false;
        this.interrupted = false;
    }

    public void tryToPark() {
        LockSupport.park();
        this.interrupted = Thread.interrupted();
    }

    public String toString() {
        return "Implementation by method 'LockSupport.park()'";
    }

    //****************************************************************************************************************//
    //                                           object park Implement                                                //
    //****************************************************************************************************************//
    static class ThreadParkSupport2 extends ThreadParkSupport {
        ThreadParkSupport2(Object blocker) {
            this.blockObject = blocker;
        }

        public String toString() {
            return "Implementation by method 'LockSupport.park(blockObject)'";
        }

        public final void tryToPark() {
            LockSupport.park(blockObject);
            this.interrupted = Thread.interrupted();
        }
    }

    //****************************************************************************************************************//
    //                                            NanoSeconds park Implement                                          //
    //****************************************************************************************************************//
    static class NanoSecondsParkSupport extends ThreadParkSupport {
        private final long nanoTime;//used in reset method

        NanoSecondsParkSupport(long nanoTime) {
            this.nanoTime = nanoTime;
            this.deadlineTime = System.nanoTime() + nanoTime;
        }

        public final void reset() {
            super.reset();
            this.deadlineTime = System.nanoTime() + nanoTime;
        }

        public void tryToPark() {
            this.parkNanos = deadlineTime - System.nanoTime();
            if (this.parkNanos > 0L) {
                parkNanos(parkNanos);
                this.interrupted = Thread.interrupted();
            } else {
                this.hasTimeout = true;
            }
        }

        public String toString() {
            return "Implementation by  method 'LockSupport.parkNanos(time)'";
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

        public final void tryToPark() {
            this.parkNanos = deadlineTime - System.nanoTime();
            if (this.parkNanos > 0L) {
                parkNanos(blockObject, parkNanos);
                this.interrupted = Thread.interrupted();
            } else {
                this.hasTimeout = true;
            }
        }

        public String toString() {
            return "Implementation by method 'LockSupport.parkNanos(blockObject,time)'";
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds park Implement                                              //
    //****************************************************************************************************************//
    static class UtilMillsParkSupport1 extends ThreadParkSupport {

        UtilMillsParkSupport1(long deadline) {
            this.deadlineTime = deadline;
        }

        public void tryToPark() {
            this.parkNanos = MILLISECONDS.toNanos(deadlineTime - System.currentTimeMillis());
            if (this.parkNanos > 0L) {
                parkUntil(parkNanos);
                this.interrupted = Thread.interrupted();
            } else {
                this.hasTimeout = true;
            }
        }

        public final void reset() {
            throw new IllegalArgumentException("can't support absolute time reset");
        }

        public String toString() {
            return "Implementation by method 'LockSupport.parkUntil(milliseconds)'";
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds Park util Implement                                         //
    //****************************************************************************************************************//
    static class UtilMillsParkSupport2 extends UtilMillsParkSupport1 {
        UtilMillsParkSupport2(long deadline, Object blocker) {
            super(deadline);
            this.blockObject = blocker;
        }

        public final void tryToPark() {
            this.parkNanos = MILLISECONDS.toNanos(deadlineTime - System.currentTimeMillis());
            if (this.parkNanos > 0L) {
                parkUntil(blockObject, parkNanos);
                this.interrupted = Thread.interrupted();
            } else {
                this.hasTimeout = true;
            }
        }

        public String toString() {
            return "Implementation by method 'LockSupport.parkUntil(blockObject,milliseconds)'";
        }
    }
}



