/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.permit.impl;

import org.jmin.permit.PermitPool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronizePermitPool extends SynchronizeWaitChain implements PermitPool {
    private boolean fair;
    private boolean reentrant;
    private int permitMaxSize;
    private AtomicInteger permitSize;

    public SynchronizePermitPool(int permits) {
        this(permits, false);
    }

    public SynchronizePermitPool(int permits, boolean fair) {
        this(permits, fair, true);
    }

    public SynchronizePermitPool(int permits, boolean fair, boolean reentrant) {
        if (permits <= 0) throw new IllegalArgumentException("permit size must be greater than zero");

        this.fair = fair;
        this.reentrant = reentrant;
        this.permitMaxSize = permits;
        this.permitSize = new AtomicInteger(permitMaxSize);
    }

    //true,fair mode to acquire permit
    public boolean isFair() {
        return fair;
    }

    //true,reentrant
    public boolean isReentrant() {
        return reentrant;
    }

    //max size pooled permit
    public int getPermitMaxSize() {
        return permitMaxSize;
    }

    //available permit size in pool
    public int getPermitSize() {
        return permitSize.get();
    }

    //release a permit to pool
    public boolean release() {
        return true;
    }

    public boolean acquireUninterruptibly(long deadlineNs) {
        return true;
    }

    public boolean acquire(long deadlineNs) throws InterruptedException {
        return true;
    }

    private static class HoldCount {
        private int count;
    }
}
