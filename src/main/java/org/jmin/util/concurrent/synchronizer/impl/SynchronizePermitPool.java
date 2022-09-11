/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl;

import org.jmin.util.concurrent.synchronizer.PermitPool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Chris Liao
 * @version 1.0
 * for (ReentrantLock2,Semaphore2)
 */
public class SynchronizePermitPool extends SynchronizeWaitChain implements PermitPool {
    private boolean fair;
    private int permitMaxSize;
    private AtomicInteger permitSize;
    private AtomicInteger sharedCount;

    public SynchronizePermitPool(int permits) {
        this(permits, false);
    }

    public SynchronizePermitPool(int permits, boolean fair) {
        this.fair = fair;
        this.permitMaxSize = permits;
        this.permitSize = new AtomicInteger(permitMaxSize);
        if (permits <= 0) throw new IllegalArgumentException("permit size must be greater than zero");
    }

    //true,fair mode to acquire permit
    public boolean isFair() {
        return fair;
    }

    //max size pooled permit
    public int getPermitMaxSize() {
        return permitMaxSize;
    }

    //available permit size in pool
    public int getPermitSize() {
        return permitSize.get();
    }

    //acquired count with share mode(shareAcquire==true)
    public int getSharedCount() {
        return sharedCount.get();
    }

    //permit acquired by shared mode then return true
    public boolean hasSharedPermit() {
        return getSharedCount() > 0;
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
