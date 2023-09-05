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

public interface ThreadParkSupport {

    //true:timeout or interrupted
    boolean park();

    boolean isTimeout();

    boolean isInterrupted();

    long getLastParkNanos();
}
