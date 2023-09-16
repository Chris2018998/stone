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

import org.stone.shine.util.concurrent.synchronizer.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.ResultWaitPool;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CountDownLatch,a synchronization impl by wait pool,which can be regarded as a theater
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class CountDownLatch implements ResultCall {
    private AtomicInteger count;
    private ResultWaitPool waitPool;

    //create wait pool in constructor
    public CountDownLatch(int count) {
        if (count <= 0) throw new IllegalArgumentException("count <= 0");
        this.count = new AtomicInteger(count);
        this.waitPool = new ResultWaitPool();
    }

    //****************************************************************************************************************//
    //                                      1: wait methods                                                           //
    //****************************************************************************************************************//
    public void await() throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setPropagatedOnSuccess(true);
            waitPool.get(this, null, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //do nothing,the last item may be over at timeout point
        }
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig(timeout, unit);
            config.setPropagatedOnSuccess(true);
            waitPool.get(this, null, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //do nothing,the last item may be over at timeout point
        }
        return count.get() == 0;
    }

    //****************************************************************************************************************//
    //                                          2: method of count and count down                                     //
    //****************************************************************************************************************//
    public int getCount() {
        return count.get();
    }

    //method of result call
    public Object call(Object arg) {
        return count.get() == 0;
    }

    public void countDown() {
        int c;
        do {
            c = this.count.get();
            if (c == 0) return;
            if (this.count.compareAndSet(c, c - 1)) {
                if (c == 1) waitPool.wakeupFirst();
                return;
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          3: desc of instance                                                   //
    //****************************************************************************************************************//
    public String toString() {
        return super.toString() + "[Count = " + count.get() + "]";
    }
}
