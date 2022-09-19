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

import static org.jmin.stone.synchronizer.impl.SynchronizedNodeState.WAIT_SHARED;

/**
 * @author Chris Liao
 * @version 1.0
 */

public class SynchronizedWaitPool implements WaitWakeupPool {

    private SynchronizedNodeChain chain;

    public void wakeupWaiting() {

    }

    public boolean testCondition() {
        return false;
    }

    public void await() throws InterruptedException {
        await(0);
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await(System.nanoTime() + unit.toNanos(timeout));
    }

    private boolean await(long deadlineNs) throws InterruptedException {
        if (testCondition()) {
            return true;
        } else {
            SynchronizedNode node = new SynchronizedNode(WAIT_SHARED);
            chain.addNode(node);


            return true;
        }
    }
}
