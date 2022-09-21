/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.WaitWakeupPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import static org.jmin.stone.synchronizer.impl.SynchronizedNodeState.WAIT_SHARED;

/**
 * @author Chris Liao
 * @version 1.0
 */

public abstract class SynchronizedWaitPool implements WaitWakeupPool {

    private final SynchronizedNodeChain chain;//@todo need dev

    public SynchronizedWaitPool() {
        chain = new SynchronizedNodeChain();
    }

    public abstract boolean testCondition();

    public abstract void resetCondition();

    protected void wakeupWaiting() {
        //@todo pop node and set them to ACQUIRE_SUCCESS and unpark them
    }

    public void doAwait() throws InterruptedException {
        try {
            doAwait(0);
        } catch (TimeoutException e) {
        }
    }

    public void doAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        doAwait(unit.toNanos(timeout));
    }

    private void doAwait(long timeoutNs) throws InterruptedException, TimeoutException {
        if (testCondition()) {//match
            return;//do nothing
        } else {
            SynchronizedNode node = new SynchronizedNode(WAIT_SHARED);
            chain.addNode(node);
            if (timeoutNs > 0)
                LockSupport.parkNanos(timeoutNs);
            else
                LockSupport.park();

            //interrupted then throws InterruptedException
            if (node.getThread().isInterrupted()) {
                chain.removeNode(node);
                throw new InterruptedException();
            } else if (node.getState() != SynchronizedNodeState.ACQUIRE_SUCCESS) {
                // state == ACQUIRE_SUCCESS value set from<method> wakeupWaiting</method>
                chain.removeNode(node);
                throw new TimeoutException();
            }
        }
    }
}
