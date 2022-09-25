/*
 * Copyright(C) Chris2018998
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
//                                                1:LockSupport.park                                                  //
//********************************************************************************************************************//
class ThreadParker {
    //park time,milliseconds(time point) or nanoSeconds(time value)
    protected long time;
    //timeout flag
    protected boolean timeout;
    //park block object
    protected Object blocker;

    ThreadParker(long time) {
        this.time = time;
    }

    ThreadParker(long time, Object blocker) {
        this.time = time;
        this.blocker = blocker;
    }

    //****************************************static methods(begin)***************************************************//
    public static ThreadParker create(long time, boolean isMilliseconds) {
        if (time <= 0)
            return new ThreadParker(0);
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
    //****************************************static methods(end)*****************************************************//

    public void calNexTime() {
    }

    //invoke one of methods(LockSupport.park,LockSupport.parkUntil,LockSupport.parkNanos)
    public void park() {
        LockSupport.park();
    }

    public long getTime() {
        return time;
    }

    public boolean isTimeout() {
        return timeout;
    }

    //***********************************************sub class********************************************************//
    private static class ThreadObjectParker extends ThreadParker {
        ThreadObjectParker(Object blocker) {
            super(0, blocker);
        }

        public void park() {
            LockSupport.park(blocker);
        }
    }

    //****************************************************************************************************************//
    //                                           2:LockSupport.parkNanos                                              //
    //****************************************************************************************************************//
    private static class NanoSecondsParker extends ThreadParker {
        private long deadlineNs;

        NanoSecondsParker(long time) {
            super(time);
            this.deadlineNs = System.nanoTime() + time;
        }

        NanoSecondsParker(long time, Object blocker) {
            super(time, blocker);
            this.deadlineNs = System.nanoTime() + time;
        }

        public void calNexTime() {
            this.time = deadlineNs - System.nanoTime();
            this.timeout = time <= 0;
        }

        public void park() {
            LockSupport.parkNanos(time);
        }
    }

    private static class NanoSecondsObjectParker extends NanoSecondsParker {
        NanoSecondsObjectParker(long time, Object blocker) {
            super(time, blocker);
        }

        public void park() {
            LockSupport.parkNanos(blocker, time);
        }
    }

    //****************************************************************************************************************//
    //                                           3:LockSupport.parkUntil                                              //
    //****************************************************************************************************************//
    private static class MillisecondsUtilParker extends ThreadParker {
        MillisecondsUtilParker(long time) {
            super(time);
        }

        MillisecondsUtilParker(long time, Object blocker) {
            super(time, blocker);
        }

        public void calNexTime() {
            this.timeout = System.currentTimeMillis() >= time;
        }

        public void park() {
            LockSupport.parkUntil(time);
        }
    }

    private static class MillisecondsObjectUtilParker extends MillisecondsUtilParker {
        MillisecondsObjectUtilParker(long time, Object blocker) {
            super(time, blocker);
        }

        public void park() {
            LockSupport.parkUntil(blocker, time);
        }
    }
}