/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import static org.jmin.stone.synchronizer.impl.ThreadNodeState.WAITING;

/**
 * Wait Pool,threads will leave from pool under three situation
 * 1: wakeup by other thread
 * 2: wait timeout
 * 3: wait interrupted
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class ThreadWaitPool extends ThreadNodeChain {
    //****************************************************************************************************************//
    //                                          abstract methods                                                      //
    //****************************************************************************************************************//
    //if true,<method>{@link #wakeupWaiting}</method>should be called by manually or auto
    public abstract boolean testCondition();

    //reset condition to initialization state
    public abstract void resetCondition();

    //****************************************************************************************************************//
    //                                           wait/wakeup methods                                                  //
    //****************************************************************************************************************//
    //wakeup all threads in pool and clear pool
    protected void wakeupWaiting() {


    }

    /**
     * Current thread join pool,then blocked,then waiting util wake-up from other thread
     *
     * @throws InterruptedException interrupted during waiting
     */
    protected void doWait() throws InterruptedException {
        try {
            doWait(0);
        } catch (TimeoutException e) {
            //do nothing
        }
    }

    /**
     * Current thread join pool,then blocked,then waiting util wake-up from other thread
     *
     * @throws InterruptedException interrupted during waiting
     * @throws TimeoutException     wait timeout with specified time value
     */
    protected void doWait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        doWait(unit.toNanos(timeout));
    }

    private void doWait(long timeoutNs) throws InterruptedException, TimeoutException {
        if (testCondition()) {//match
            return;//do nothing
        } else {
            ThreadNode node = new ThreadNode(WAITING);
            this.addNode(node);
            if (timeoutNs > 0)
                LockSupport.parkNanos(timeoutNs);
            else
                LockSupport.park();

            //interrupted then throws InterruptedException
            if (node.getThread().isInterrupted()) {
                this.removeNode(node);
                throw new InterruptedException();
            } else if (node.getState() != ThreadNodeState.ACQUIRED_SUCCESS) {
                // state == ACQUIRE_SUCCESS value set from<method> wakeupWaiting</method>
                this.removeNode(node);
                throw new TimeoutException();
            }
        }
    }
}
