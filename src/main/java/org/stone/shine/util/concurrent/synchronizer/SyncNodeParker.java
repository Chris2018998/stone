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
import static java.util.concurrent.locks.LockSupport.*;

/**
 * Time parker class,supply three Implementation with park methods of {@link LockSupport} class
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
 *       SyncNodeParker parker = SyncNodeParker.create();
 *       await(parker);
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      SyncNodeParker parker = SyncNodeParker.create(nanosTimeout,false);
 *      await(parker);
 *    }
 *    public final boolean awaitUntil(Date deadlineNanos) throws InterruptedException {
 *      SyncNodeParker parker = SyncNodeParker.create(deadlineNanos.getParkNanos(),true);
 *      await(parker);
 *    }
 *   public final boolean await(long parkNanos, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      SyncNodeParker parker = SyncNodeParker.create(nanosTimeout,false);
 *      await(parker);
 *   }
 *   //spin control
 *   private void await(SyncNodeParker  parker)throws InterruptedException {
 *      //spin source code.........
 *   }
 *  }//class end
 * }//code tag
 * </pre>
 *
 * @author Chris Liao
 * @version 1.0
 */

public class SyncNodeParker {
    long parkNanos;//value of last park time
    long deadlineTime;//time point(nanoseconds or milliseconds),value greater than 0,current is a time park

    boolean hasTimeout;
    boolean interrupted;

    SyncNodeParker() {
    }

    public final boolean isTimeout() {
        return hasTimeout;
    }

    public final boolean isInterrupted() {
        return interrupted;
    }

    public final long getLastParkNanos() {
        return parkNanos;
    }

    public boolean tryPark() {
        park(this);
        return this.interrupted = Thread.interrupted();
    }

    public String toString() {
        return "Implementation by method 'LockSupport.park(blocker)'";
    }

    //****************************************************************************************************************//
    //                                            NanoSeconds park Implement                                          //
    //****************************************************************************************************************//
    static class NanoSecondsParkSupport extends SyncNodeParker {

        NanoSecondsParkSupport(long nanoTime) {
            this.deadlineTime = System.nanoTime() + nanoTime;
        }

        public boolean tryPark() {
            if ((this.parkNanos = deadlineTime - System.nanoTime()) > 0L) {
                parkNanos(this, parkNanos);
                return this.interrupted = Thread.interrupted();
            } else {
                return this.hasTimeout = true;
            }
        }

        public String toString() {
            return "Implementation by  method 'LockSupport.parkNanos(blocker,time)'";
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds park Implement                                              //
    //****************************************************************************************************************//
    static class UtilMillsParkSupport extends SyncNodeParker {

        UtilMillsParkSupport(long deadlineTime) {
            this.deadlineTime = deadlineTime;
        }

        public boolean tryPark() {
            this.parkNanos = MILLISECONDS.toNanos(deadlineTime - System.currentTimeMillis());
            if (this.parkNanos > 0L) {
                parkUntil(this, deadlineTime);
                return this.interrupted = Thread.interrupted();
            } else {
                return this.hasTimeout = true;
            }
        }

        public String toString() {
            return "Implementation by method 'LockSupport.parkUntil(blocker,milliseconds)'";
        }
    }
}
