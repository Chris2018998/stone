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
 *       ThreadParkSupport parker = ThreadParkSupport.create(0,false);
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
//                                           1:LockSupport.park                                                       //
//********************************************************************************************************************//
public class ThreadParkSupport {
    private static final long spinForTimeoutThreshold = 1000L;
    protected long deadline;
    protected long parkTime;//timeout check: this value less than zero or equals zero
    protected Object blocker;
    protected boolean interrupted;

    //****************************************************************************************************************//
    //                                              constructors(2)                                                   //
    //****************************************************************************************************************//
    ThreadParkSupport() {
    }

    ThreadParkSupport(Object blocker) {
        this.blocker = blocker;
        this.parkTime = 1;//set dummy value for park(),which means never timeout
    }

    //****************************************************************************************************************//
    //                                           Factory  methods(begin)                                              //
    //****************************************************************************************************************//
    public static ThreadParkSupport create(long time, boolean isMilliseconds) {
        if (time <= 0)
            return new ThreadParkSupport();
        else if (isMilliseconds)
            return new MillisecondsUtilParkSupport(time);
        else
            return new NanoSecondsParkSupport(time);
    }

    public static ThreadParkSupport create(long time, boolean isMilliseconds, Object blocker) {
        if (time <= 0)
            return new ThreadObjectParkSupport(blocker);
        else if (isMilliseconds)
            return new MillisecondsObjectUtilParkSupport(time, blocker);
        else
            return new NanoSecondsObjectParkSupport(time, blocker);
    }
    //****************************************************************************************************************//
    //                                           Factory  methods(end)                                                //
    //****************************************************************************************************************//

    public boolean park() {
        LockSupport.park();
        return interrupted = Thread.interrupted();
    }

    public long getDeadline() {
        return deadline;
    }

    public long getParkTime() {
        return parkTime;
    }

    public boolean isTimeout() {
        return parkTime > 0;
    }

    //true,thread can be parked
    public boolean calculateParkTime() {
        return true;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    //blocker implementation sub class
    private static class ThreadObjectParkSupport extends ThreadParkSupport {
        ThreadObjectParkSupport(Object blocker) {
            super(blocker);
        }

        public boolean park() {
            LockSupport.park(blocker);
            return interrupted = Thread.interrupted();
        }
    }

    //****************************************************************************************************************//
    //                                           2: LockSupport.parkNanos                                             //
    //****************************************************************************************************************//
    private static class NanoSecondsParkSupport extends ThreadParkSupport {
        NanoSecondsParkSupport(long nanoTime) {
            this.deadline = System.nanoTime() + nanoTime;
        }

        //true means that time point before deadline
        public boolean calculateParkTime() {
            this.parkTime = deadline - System.nanoTime();
            return parkTime > 0;
        }

        public boolean park() {
            if (parkTime > spinForTimeoutThreshold) {
                LockSupport.parkNanos(parkTime);
                return interrupted = Thread.interrupted();
            } else {
                return interrupted;
            }
        }
    }

    private static class NanoSecondsObjectParkSupport extends NanoSecondsParkSupport {
        NanoSecondsObjectParkSupport(long nanoTime, Object blocker) {
            super(nanoTime);
            this.blocker = blocker;
        }

        public boolean park() {
            if (parkTime > spinForTimeoutThreshold) {
                LockSupport.parkNanos(parkTime);
                return interrupted = Thread.interrupted();
            } else {
                return interrupted;
            }
        }
    }

    //****************************************************************************************************************//
    //                                           3: LockSupport.parkUntil                                             //
    //****************************************************************************************************************//
    private static class MillisecondsUtilParkSupport extends ThreadParkSupport {
        MillisecondsUtilParkSupport(long deadline) {
            this.deadline = deadline;
        }

        public boolean calculateParkTime() {
            this.parkTime = deadline - System.currentTimeMillis();
            return parkTime > 0;
        }

        public boolean park() {
            LockSupport.parkUntil(deadline);
            return interrupted = Thread.interrupted();
        }
    }

    private static class MillisecondsObjectUtilParkSupport extends MillisecondsUtilParkSupport {
        MillisecondsObjectUtilParkSupport(long deadline, Object blocker) {
            super(deadline);
            this.blocker = blocker;
        }

        public boolean park() {
            LockSupport.parkUntil(blocker, deadline);
            return interrupted = Thread.interrupted();
        }
    }
}



