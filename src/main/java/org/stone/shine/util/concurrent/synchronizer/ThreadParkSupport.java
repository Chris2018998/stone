/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer;

import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;
import static java.util.concurrent.locks.LockSupport.parkUntil;

/**
 * Time park class,supply three Implementation with park methods of {@link LockSupport} class
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
 *       ThreadParkSupport park = ThreadParkSupport.create();
 *       await(park);
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      ThreadParkSupport park = ThreadParkSupport.create(nanosTimeout,false);
 *      await(park);
 *    }
 *    public final boolean awaitUntil(Date deadlineNanos) throws InterruptedException {
 *      ThreadParkSupport park = ThreadParkSupport.create(deadlineNanos.getParkNanos(),true);
 *      await(park);
 *    }
 *   public final boolean await(long parkNanos, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      ThreadParkSupport park = ThreadParkSupport.create(nanosTimeout,false);
 *      await(park);
 *   }
 *   //spin control
 *   private void await(ThreadParkSupport  park)throws InterruptedException {
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
    protected boolean interrupted;

    ThreadParkSupport() {
    }

    public void computeAndPark() {
        LockSupport.park(this);
        this.interrupted = Thread.interrupted();
    }

    public long getLastParkNanos() {
        return 0L;
    }

    public boolean isTimeout() {
        return false;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public String toString() {
        return "Implementation by method 'LockSupport.park(blocker)'";
    }

    //****************************************************************************************************************//
    //                                          1: LockSupport.parkNanos                                              //
    //****************************************************************************************************************//
    static final class NanosParkSupport extends ThreadParkSupport {
        private final long deadlineTime;
        private long parkNanos;
        private boolean hasTimeout;

        NanosParkSupport(long parkNanos) {
            this.deadlineTime = System.nanoTime() + parkNanos;
        }

        public void computeAndPark() {
            if ((this.parkNanos = deadlineTime - System.nanoTime()) > 0L) {
                parkNanos(this, parkNanos);
                this.interrupted = Thread.interrupted();
            } else {
                this.hasTimeout = true;
            }
        }

        public boolean isTimeout() {
            return hasTimeout;
        }

        public long getLastParkNanos() {
            return parkNanos;
        }

        public String toString() {
            return "Implementation by  method 'LockSupport.parkNanos(blocker,time)'";
        }
    }

    //****************************************************************************************************************//
    //                                          2: LockSupport.parkUntil                                              //
    //****************************************************************************************************************//
    static final class DateUtilParkSupport extends ThreadParkSupport {
        private final long deadlineTime;
        private long parkNanos;
        private boolean hasTimeout;

        DateUtilParkSupport(long deadlineTime) {
            this.deadlineTime = deadlineTime;
        }

        public void computeAndPark() {
            this.parkNanos = MILLISECONDS.toNanos(deadlineTime - System.currentTimeMillis());
            if (this.parkNanos > 0L) {
                parkUntil(this, deadlineTime);
                this.interrupted = Thread.interrupted();
            } else {
                this.hasTimeout = true;
            }
        }

        public boolean isTimeout() {
            return hasTimeout;
        }

        public long getLastParkNanos() {
            return parkNanos;
        }

        public String toString() {
            return "Implementation by method 'LockSupport.parkUntil(blocker,milliseconds)'";
        }
    }
}
