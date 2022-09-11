/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl;

import org.jmin.util.concurrent.synchronizer.PermitPool;

/**
 * @author Chris Liao
 * @version 1.0
 * for (ReentrantLock2,Semaphore2)
 */
public class SynchronizePermitPool extends SynchronizeWaitQueue implements PermitPool {

    //true,fair mode to acquire permit
    public boolean isFair() {
        return true;
    }

    //available permit size in pool
    public int getPermitSize() {
        return 1;
    }

    //acquired count with share mode(shareAcquire==true)
    public int getSharedCount() {
        return 1;
    }

    //permit acquired by shared mode then return true
    public boolean isInShared() {
        return true;
    }

    //plugin method after permit acquired successful
    public void afterAcquired(boolean acquiredShare) {
    }

    //plugin method after permit released successful
    public void afterReleased(boolean acquiredShare) {
    }

    //release a permit to pool
    public boolean release() {
        return true;
    }

    public boolean acquireUninterruptibly(boolean shareAcquire, long deadlineNs) {
        return true;
    }

    public boolean acquire(boolean shareAcquire, long deadlineNs) throws InterruptedException {
        return true;
    }
}
