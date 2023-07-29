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

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.locks.LockSupport.*;
import static org.stone.tools.CommonUtil.spinForTimeoutThreshold;

/**
 * Time parker class,supply three Implementation with park methods of {@link @java.util.concurrent.locks.LockSupport} class
 * 1:park
 * 2:blockNanos
 * 3:parkUntil
 *
 * <p>
 * Class objective:reduce spin similar code (for example:some await methods in class:{@link @java.util.concurrent.locks.AbstractQueuedSynchronizer})
 * Usage demo:
 * <pre>{@code
 * class ConditionX {
 *    public final void await() throws InterruptedException {
 *       SyncParkSupport parker = SyncParkSupport.create();
 *       await(parker);
 *    }
 *    public final void await(long nanosTimeout) throws InterruptedException {
 *      SyncParkSupport parker = SyncParkSupport.create(nanosTimeout,false);
 *      await(parker);
 *    }
 *    public final boolean awaitUntil(Date deadlineNanos) throws InterruptedException {
 *      SyncParkSupport parker = SyncParkSupport.create(deadlineNanos.getBlockNanos(),true);
 *      await(parker);
 *    }
 *   public final boolean await(long blockNanos, TimeUnit unit)throws InterruptedException {
 *      long nanosTimeout = unit.toNanos(timeout);
 *      SyncParkSupport parker = SyncParkSupport.create(nanosTimeout,false);
 *      await(parker);
 *   }
 *   //spin control
 *   private void await(SyncParkSupport  parker)throws InterruptedException {
 *      //spin source code.........
 *   }
 *  }//class end
 * }//code tag
 * </pre>
 *
 * @author Chris Liao
 * @version 1.0
 */

public class SyncParkSupport {
    long blockNanos = spinForTimeoutThreshold + 1;//compute before parking,if less than zero or equals zero means timeout
    long deadlineNanos;//nanoseconds
    Object blockObject;
    boolean interrupted;

    SyncParkSupport() {
    }

    public final long getBlockNanos() {
        return blockNanos;
    }

    public final long getDeadlineNanos() {
        return deadlineNanos;
    }

    public final boolean isTimeout() {
        return blockNanos <= 0;
    }

    public final boolean isInterrupted() {
        return interrupted;
    }

    public String toString() {
        return "Implementation with method 'park()'";
    }

    public void reset() {
        this.interrupted = false;
    }

    public long computeBlockTime() {
        return blockNanos;
    }

    public boolean block() {
        park();
        return this.interrupted = Thread.interrupted();
    }

    //****************************************************************************************************************//
    //                                           object block Implement                                               //
    //****************************************************************************************************************//
    static class ThreadBlockSupport2 extends SyncParkSupport {
        ThreadBlockSupport2(Object blocker) {
            this.blockObject = blocker;
        }

        public final boolean block() {
            park(blockObject);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'park(blockObject)'";
        }
    }

    //****************************************************************************************************************//
    //                                            NanoSeconds block Implement                                         //
    //****************************************************************************************************************//
    static class NanoSecondsBlockSupport extends SyncParkSupport {
        private final long nanoTime;

        NanoSecondsBlockSupport(long nanoTime) {
            this.nanoTime = nanoTime;
            this.deadlineNanos = System.nanoTime() + nanoTime;
        }

        public final void reset() {
            super.reset();
            this.deadlineNanos = System.nanoTime() + nanoTime;
        }

        public long computeBlockTime() {
            return this.blockNanos = deadlineNanos - System.nanoTime();
        }

        public boolean block() {
            parkNanos(blockNanos);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'parkNanos(time)'";
        }
    }

    //****************************************************************************************************************//
    //                                    NanoSeconds blockObject park Implement                                      //
    //****************************************************************************************************************//
    static class NanoSecondsBlockSupport2 extends NanoSecondsBlockSupport {
        NanoSecondsBlockSupport2(long nanoTime, Object blocker) {
            super(nanoTime);
            this.blockObject = blocker;
        }

        public final boolean block() {
            parkNanos(blockObject, blockNanos);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'parkNanos(time,blockObject)'";
        }
    }

    //****************************************************************************************************************//
    //                                       MilliSeconds block Implement                                             //
    //****************************************************************************************************************//
    static class UtilMillsBlockSupport1 extends SyncParkSupport {
        final long deadlineMillis;//nanoseconds or milliseconds

        UtilMillsBlockSupport1(long deadline) {
            this.deadlineMillis = deadline;
            this.deadlineNanos = TimeUnit.MILLISECONDS.toNanos(deadlineMillis);//nanoseconds
        }

        public final long computeBlockTime() {
            return this.blockNanos = deadlineNanos - System.nanoTime();
        }

        public boolean block() {
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
    //                                       MilliSeconds block util Implement                                        //
    //****************************************************************************************************************//
    static class UtilMillsBlockSupport2 extends UtilMillsBlockSupport1 {
        UtilMillsBlockSupport2(long deadline, Object blocker) {
            super(deadline);
            this.blockObject = blocker;
        }

        public boolean block() {
            parkUntil(blockObject, deadlineMillis);
            return this.interrupted = Thread.interrupted();
        }

        public String toString() {
            return "Implementation with method 'parkUntil(milliseconds,blockObject)'";
        }
    }
}



