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

import org.stone.shine.synchronizer.extend.AcquireTypes;
import org.stone.shine.synchronizer.extend.ResourceWaitPool;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * ReadWrite Lock Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ReentrantReadWriteLock implements ReadWriteLock {
    private static final int SHARED_SHIFT = 16;
    private static final int SHARED_UNIT = (1 << SHARED_SHIFT);
    private static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
    private static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

    //Inner class providing readLock
    private final ReentrantReadWriteLock.ReadLock readerLock;
    //Inner class providing writeLock
    private final ReentrantReadWriteLock.WriteLock writerLock;

    //****************************************************************************************************************//
    //                                          1: constructors (2)                                                   //
    //****************************************************************************************************************//
    public ReentrantReadWriteLock() {
        this(false);
    }

    public ReentrantReadWriteLock(boolean fair) {
        LockAtomicState lockState = new LockAtomicState();
        ResourceWaitPool waitPool = new ResourceWaitPool(fair);
        this.writerLock = new WriteLock(waitPool, new WriteLockAction(lockState));
        this.readerLock = new ReadLock(waitPool, new ReadLockAction(lockState));
    }

    //****************************************************************************************************************//
    //                                          2:static methods(6)                                                   //
    //****************************************************************************************************************//
    private static int sharedCount(int c) {
        return c >>> SHARED_SHIFT;
    }

    private static int exclusiveCount(int c) {
        return c & EXCLUSIVE_MASK;
    }

    private static int incrementExclusiveCount(int c) {
        return c + 1;
    }

    private static int decrementExclusiveCount(int c) {
        return c - 1;
    }

    private static int incrementSharedCount(int c) {
        return c + SHARED_UNIT;
    }

    private static int decrementSharedCount(int c) {
        return c - SHARED_UNIT;
    }

    //****************************************************************************************************************//
    //                                       3:readLock/writeLock                                                     //                                                                                  //
    //****************************************************************************************************************//
    //Returns the lock used for reading.
    public Lock readLock() {
        return readerLock;
    }

    //Returns the lock used for writing.
    public Lock writeLock() {
        return writerLock;
    }

    //****************************************************************************************************************//
    //                                      4: SharedHoldCounter Impl                                                 //                                                                                  //
    //****************************************************************************************************************//
    private static class SharedHoldCounter {
        private int holdCount = 0;
    }

    private static class SharedHoldThreadLocal extends ThreadLocal<SharedHoldCounter> {
        protected SharedHoldCounter initialValue() {
            return new SharedHoldCounter();
        }
    }

    //****************************************************************************************************************//
    //                                      5: WriteLock/ReadLock Impl                                                //                                                                                  //
    //****************************************************************************************************************//
    private static class WriteLock extends BaseLock {
        WriteLock(ResourceWaitPool waitPool, LockAction lockAction) {
            super(waitPool, lockAction, AcquireTypes.TYPE_Exclusive);
        }
    }

    private static class ReadLock extends BaseLock {
        ReadLock(ResourceWaitPool waitPool, LockAction lockAction) {
            super(waitPool, lockAction, AcquireTypes.TYPE_Exclusive);
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    //****************************************************************************************************************//
    //                                       6: Lock Action Impl                                                      //
    //****************************************************************************************************************//
    private static class WriteLockAction extends LockAction {
        WriteLockAction(LockAtomicState lockState) {
            super(lockState);
        }

        public Object call(Object size) {
            int state = lockState.getState();
            if (state == 0) {
                if (lockState.compareAndSetState(0, 1)) {
                    lockState.setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                } else {
                    return false;
                }
            } else if (lockState.getExclusiveOwnerThread() == Thread.currentThread()) {//Reentrant
                state = incrementExclusiveCount(state);
                if (state > MAX_COUNT) throw new Error("Maximum lock count exceeded");
                lockState.setState(state);
                return true;
            } else {
                return false;
            }
        }

        public boolean tryRelease(int size) {
            if (lockState.getExclusiveOwnerThread() == Thread.currentThread()) {
                int curState = lockState.getState();
                int writeCount = exclusiveCount(curState);

                if (writeCount == 1) lockState.setExclusiveOwnerThread(null);
                if (writeCount > 0) lockState.setState(decrementExclusiveCount(curState));
                return writeCount == 1;
            } else {
                return false;
            }
        }
    }

    private static class ReadLockAction extends LockAction {
        ReadLockAction(LockAtomicState lockState) {
            super(lockState);
        }

        public Object call(Object size) {
            //@todo
            return true;
        }

        public boolean tryRelease(int size) {
            //@todo
            return true;
        }
    }
}
