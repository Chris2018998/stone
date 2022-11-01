/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.synchronizer.extend;

import org.stone.shine.synchronizer.base.ResultWaitPool;

/**
 * @author Chris Liao
 * @version 1.0
 */

public final class PermitPool extends ResultWaitPool {
    private final int permits;

    public PermitPool(int permits) {
        this(permits, false);
    }

    public PermitPool(int permits, boolean fair) {
        super(fair);
        this.permits = permits;
    }

    //available permit size in pool
    public int getPermitSize() {
        return permits;
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
