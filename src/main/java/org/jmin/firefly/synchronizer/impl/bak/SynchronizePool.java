/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.firefly.synchronizer.impl.bak;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronizePool extends SynchronizeWaitChain implements Pool {
    private boolean fair;
    private boolean reentrant;
    private int permitMaxSize;
    private AtomicInteger permitSize;

    public SynchronizePool(int permits) {
        this(permits, false);
    }

    public SynchronizePool(int permits, boolean fair) {
        this(permits, fair, true);
    }

    public SynchronizePool(int permits, boolean fair, boolean reentrant) {
        if (permits <= 0) throw new IllegalArgumentException("synchronizer size must be greater than zero");

        this.fair = fair;
        this.reentrant = reentrant;
        this.permitMaxSize = permits;
        this.permitSize = new AtomicInteger(permitMaxSize);
    }

    //true,fair mode to acquire synchronizer
    public boolean isFair() {
        return fair;
    }

    //true,reentrant
    public boolean isReentrant() {
        return reentrant;
    }

    //max size pooled synchronizer
    public int getPermitMaxSize() {
        return permitMaxSize;
    }

    //available synchronizer size in pool
    public int getPermitSize() {
        return permitSize.get();
    }

    //release a synchronizer to pool
    public void release() {

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
