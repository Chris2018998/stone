/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.locks;

import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.extend.ResourceWaitPool;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Reentrant Lock Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ReentrantLock extends ResourceWaitPool implements Lock {
    //hold count of owner thread
    private int holdCount = 0;

    //I hope to create difference,so try{@code AtomicReference}
    private AtomicReference<Thread> ownerRef = new AtomicReference<>(null);

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public ReentrantLock() {
        this(false);
    }

    public ReentrantLock(boolean fair) {
        super(null, fair);

    }

    //****************************************************************************************************************//
    //                                          2: lock methods                                                       //
    //****************************************************************************************************************//
    public void lock() {
        try {
            ThreadParkSupport support = ThreadParkSupport.create(0, false);
            super.acquire(1, support, true, null, true);
        } catch (Exception e) {
            //do nothing
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        ThreadParkSupport support = ThreadParkSupport.create(0, false);
        super.acquire(1, support, true, null, true);
    }


    public boolean tryLock() {
        return super.tryAcquire(1);
    }


    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        ThreadParkSupport support = ThreadParkSupport.create(unit.toNanos(time), false);
        return super.acquire(1, support, true, null, true);
    }


    public void unlock() {
        super.release(1);
    }


    public Condition newCondition() {
        //@todo
        return null;
        // return access.newCondition();
    }

    //****************************************************************************************************************//
    //                                          3: monitor methods                                                    //
    //****************************************************************************************************************//

    public int getHoldCount() {
        return holdCount;
    }

    public boolean isLocked() {
        return ownerRef.get() != null;
    }

    public boolean isHeldByCurrentThread() {
        return ownerRef.get() == Thread.currentThread();
    }

    protected Thread getOwner() {
        return ownerRef.get();
    }

    public boolean hasWaiters(Condition condition) {
        return true;
    }

    public int getWaitQueueLength(Condition condition) {
        return 1;
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        return null;
    }

    public String toString() {
        Thread o = ownerRef.get();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}
