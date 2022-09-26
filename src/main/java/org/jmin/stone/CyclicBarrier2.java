/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone;

import org.jmin.stone.synchronizer.impl.ThreadWaitPool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class CyclicBarrier2 extends ThreadWaitPool {
    private int size;
    private int tripCount;
    private AtomicInteger count;
    private AtomicInteger state;//0-init,1-gather,2-trip,3-broken
    private Runnable tripAction;

    public CyclicBarrier2(int size) {
        this(size, null);
    }

    public CyclicBarrier2(int size, Runnable tripAction) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.size = size;
        this.tripAction = tripAction;
        this.state = new AtomicInteger(0);
        this.count = new AtomicInteger(0);
    }

    public boolean testCondition() {
        return count.get() == size; //current barrier reach
    }

    public void resetCondition() {//for next generation
        //do nothing
    }

    public void breakBarrier() {//break barrier
        //do nothing
    }
}
