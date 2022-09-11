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
 */
public class SynchronizePermitPool extends SynchronizeWaitChain implements PermitPool {
    private boolean fair;
    private int permitMaxSize;
    private AtomicInteger permitSize;
    private AtomicInteger sharedCount;
    private ThreadLocal<HoldCount> sharedThreadLocal;
    private ThreadLocal<HoldCount> exclusiveThreadLocal;

    public SynchronizePermitPool(int permits) {
        this(permits, false);
    }

    public SynchronizePermitPool(int permits, boolean fair) {
        if (permits <= 0) throw new IllegalArgumentException("permit size must be greater than zero");

        this.fair = fair;
        this.permitMaxSize = permits;
        this.permitSize = new AtomicInteger(permitMaxSize);
        this.sharedCount = new AtomicInteger(0);
        this.sharedThreadLocal = new ThreadLocal<>();
        this.exclusiveThreadLocal = new ThreadLocal<>();
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


    //get hold shared count for current thread
    public int getHoldSharedCount() {
        HoldCount hold = sharedThreadLocal.get();
        return hold != null ? hold.count : 0;
    }

    //get hold exclusive count for current thread
    public int getHoldExclusiveCount() {
        HoldCount hold = exclusiveThreadLocal.get();
        return hold != null ? hold.count : 0;
    }

    //plugin method after permit acquired successful
    public void afterAcquired(boolean shareAcquired) {

    }

    //plugin method after permit released successful
    public void afterReleased(boolean shareAcquired) {
    }

    //release a permit to pool
    public boolean release() {
        return true;
    }

    public boolean acquireUninterruptibly(boolean shareAcquired, long deadlineNs) {
        return true;
    }

    public boolean acquire(boolean shareAcquired, long deadlineNs) throws InterruptedException {
        return true;
    }


    private static class HoldCount {
        private int count;
    }
}
