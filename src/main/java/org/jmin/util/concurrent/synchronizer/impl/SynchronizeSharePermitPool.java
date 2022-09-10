/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl;

import org.jmin.util.concurrent.synchronizer.SharePermitPool;
import org.jmin.util.concurrent.synchronizer.SynchronizeNode;

/**
 * @author Chris Liao
 * @version 1.0
 * <p>
 * for(ReentrantReadWriteLock2)
 */
public class SynchronizeSharePermitPool extends SynchronizeReusePermitPool implements SharePermitPool {

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


    public boolean isInSharingState() {
        //@todo
        return false;
    }

    public boolean isHeldExclusively() {
        //@todo
        return false;
    }

    public boolean releaseShared() {
        //@todo
        return false;
    }

    public boolean acquireSharedUninterruptibly(long deadlineNs) {
        //@todo
        return false;
    }

    public boolean acquireShared(long deadlineNs) throws InterruptedException {
        //@todo
        return false;
    }

    public void afterAcquired(SynchronizeNode node) {
        //@todo
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
