/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent;

import org.stone.shine.util.concurrent.synchronizer.ThreadBlockConfig;
import org.stone.shine.util.concurrent.synchronizer.base.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CountDownLatch
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class CountDownLatch implements ResultCall {
    //Number of programs(Remaining)
    private AtomicInteger count;
    //result call wait pool
    private ResultWaitPool waitPool;

    //create wait pool in constructor
    public CountDownLatch(int count) {
        if (count <= 0) throw new IllegalArgumentException("count <= 0");
        this.count = new AtomicInteger(count);
        this.waitPool = new ResultWaitPool();
    }

    //****************************************************************************************************************//
    //                                      1:wait methods(seat down to watch programs)                               //
    //****************************************************************************************************************//
    //wait without parkTime
    public void await() throws InterruptedException {
        try {
            waitPool.doCall(this, null, new ThreadBlockConfig());
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //do nothing,the last item may be over at timeout point
        }
    }

    //true means all programs over
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            waitPool.doCall(this, null, new ThreadBlockConfig(timeout, unit));
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //do nothing,the last item may be over at timeout point
        }
        return count.get() == 0;
    }

    //****************************************************************************************************************//
    //                                          2:count method called after one show over                             //
    //****************************************************************************************************************//
    public void countDown() {
        int c;
        do {
            c = this.count.get();
            if (c == 0) return;
            if (this.count.compareAndSet(c, c - 1)) {
                if (c == 1) waitPool.wakeupAll();//the last item over,then notify all waiters(runnable)to leave
                return;
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          3:monitor method and instance desc                                    //
    //****************************************************************************************************************//
    //monitor method,return remained number of programs
    public long getCount() {
        return count.get();
    }

    //count reach zero,which means all programs over
    public Object call(Object arg) {
        return count.get() == 0;
    }

    //Description of instance
    public String toString() {
        return super.toString() + "[Count = " + count.get() + "]";
    }
}
