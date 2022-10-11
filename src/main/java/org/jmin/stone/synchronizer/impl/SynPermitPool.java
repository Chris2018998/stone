/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.PermitPool;

import java.util.Collection;

/**
 * @author Chris Liao
 * @version 1.0
 */

public class SynPermitPool extends ThreadNodeChain implements PermitPool {
    private int permits;
    private boolean fair;

    public SynPermitPool(int permits, boolean fair) {
        this.permits = permits;
        this.fair = fair;
    }

    //true,fair mode acquisition
    public boolean isFair() {
        return fair;
    }

    //available permit size in pool
    public int getPermitSize() {
        return permits;
    }

    //Threads waiting for permit
    public Collection<Thread> getQueuedThreads() {
        return null;
    }

    //release a permit to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    public void release() {

    }

    //try to acquire a permit,if interrupted,then ignore it and continue to try
    public boolean acquireUninterruptibly(long deadlineNs) {
        return false;
    }

    //try to acquire a permit,if interrupted,then throws InterruptedException
    public boolean acquire(long deadlineNs) throws InterruptedException {
        return false;
    }
}
