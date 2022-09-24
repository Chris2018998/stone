/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone;

import org.jmin.stone.synchronizer.impl.ThreadWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class CountDownLatch2 extends ThreadWaitPool {
    private AtomicInteger count;

    public CountDownLatch2(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.count = new AtomicInteger(count);
    }

    public boolean testCondition() {
        return count.get() == 0;
    }

    public void resetCondition() {
        //do nothing
    }

    public void await() throws InterruptedException {
        super.doWait();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            super.doWait(timeout, unit);
            return testCondition();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void countDown() {
        int c;
        do {
            c = this.count.get();
            if (c == 0) return;
            if (this.count.compareAndSet(c, c - 1)) {
                if (c == 1) wakeupWaiting();
                return;
            }
        } while (true);
    }

    public long getCount() {
        return count.get();
    }

    public String toString() {
        return super.toString() + "[Count = " + count.get() + "]";
    }
}
