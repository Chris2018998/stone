/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent;

import org.stone.shine.util.concurrent.synchronizer.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.ResultWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.shine.util.concurrent.synchronizer.SyncConstants.BASE_VISIT_TESTER;
import static org.stone.shine.util.concurrent.synchronizer.validator.ResultEqualsValidator.BOOL_EQU_VALIDATOR;

/**
 * CountDownLatch,a synchronization impl by wait pool,which can be regarded as a theater
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class CountDownLatch implements ResultCall {
    private final AtomicInteger count;
    private final ResultWaitPool waitPool;

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
            waitPool.get(this, null, BOOL_EQU_VALIDATOR, BASE_VISIT_TESTER,
                    null, null, false, 0L,
                    true, true);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //do nothing,the last item may be over at timeout point
        }
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            waitPool.get(this, null, BOOL_EQU_VALIDATOR, BASE_VISIT_TESTER,
                    null, null, true, unit.toNanos(timeout),
                    true, true);
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
