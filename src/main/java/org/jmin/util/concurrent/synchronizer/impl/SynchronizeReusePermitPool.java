/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl;

import org.jmin.util.concurrent.synchronizer.ReusePermitPool;

/**
 * @author Chris Liao
 * @version 1.0
 * <p>
 * for (ReentrantLock2,Semaphore2)
 */
public class SynchronizeReusePermitPool extends SynchronizeWaitQueue implements ReusePermitPool {


    public int getSize() {
        //@todo
        return 0;
    }

    public int getMaxSize() {
        //@todo
        return 0;
    }

    public boolean isFair() {
        //@todo
        return false;
    }

    public boolean release(int args) {
        //@todo
        return false;
    }

    public boolean acquireUninterruptibly(long deadlineNs) {
        //@todo
        return false;
    }

    public boolean acquire(long deadlineNs) throws InterruptedException {
        //@todo
        return false;
    }
}
