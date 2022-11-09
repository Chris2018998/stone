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
import org.stone.shine.synchronizer.extend.ResourceLock;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Reentrant Lock Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ReentrantLock implements Lock {

    //resource access
    private ResourceLock access;

    //****************************************************************************************************************//
    //                                          1: Constructor(2)                                                     //
    //****************************************************************************************************************//
    public ReentrantLock() {
        this(false);
    }

    public ReentrantLock(boolean fair) {
        this.access = new ResourceLock(fair);
    }

    //****************************************************************************************************************//
    //                                          2: lock methods                                                       //
    //****************************************************************************************************************//
    public void lock() {
        try {
            access.acquire(ThreadParkSupport.create(0, false), false);
        } catch (Exception e) {
            //do nothing
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        try {
            access.acquire(ThreadParkSupport.create(0, false), true);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //do nothing
        }
    }

    public boolean tryLock() {
        return access.tryAcquire();
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            return access.acquire(ThreadParkSupport.create(unit.toNanos(time), false), true);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    public void unlock() {
        access.release();
    }

    public Condition newCondition() {
        return access.newCondition();
    }

    //****************************************************************************************************************//
    //                                          3: monitor methods                                                    //
    //****************************************************************************************************************//
    public int getHoldCount() {
        return access.getHoldCountByCurrentThread();
    }

    public boolean isHeldByCurrentThread() {
        return access.isHeldByCurrentThread();
    }

    public boolean isLocked() {
        return access.isHeld();
    }

    public boolean isFair() {
        return access.isFair();
    }

    protected Thread getOwner() {
        return access.getHeldThread();
    }

    public final boolean hasQueuedThreads() {
        return access.hasQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return access.hasQueuedThread(thread);
    }

    public final int getQueueLength() {
        return access.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return access.getQueuedThreads();
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
        Thread o = access.getHeldThread();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}
