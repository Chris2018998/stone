/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.locks;

import org.stone.shine.util.concurrent.synchronizer.ResultWaitPool;
import org.stone.shine.util.concurrent.synchronizer.SyncConstants;

import java.util.Collection;
import java.util.concurrent.locks.Condition;
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

    //lock state
    private final LockAtomicState lockState;
    //wait pool
    private final ResultWaitPool waitPool;
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
        this.lockState = new LockAtomicState();
        this.waitPool = new ResultWaitPool(fair);
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

    private static int incrementExclusivePart(int c, int size) {
        return c + size;
    }

    private static int decrementExclusivePart(int c, int size) {
        return c - size;
    }

    private static int incrementSharedPart(int c, int size) {
        return c + SHARED_UNIT * size;
    }

    private static int decrementSharedPart(int c, int size) {
        return c - SHARED_UNIT * size;
    }

    private static BaseLock.LockConditionImpl getLockConditionImpl(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof BaseLock.LockConditionImpl))
            throw new IllegalArgumentException("not owner");
        return (BaseLock.LockConditionImpl) condition;
    }

    //****************************************************************************************************************//
    //                                       3:readLock/writeLock                                                     //                                                                                  //
    //****************************************************************************************************************//
    //Returns the lock used for reading.
    public ReadLock readLock() {
        return readerLock;
    }

    //Returns the lock used for writing.
    public WriteLock writeLock() {
        return writerLock;
    }

    //****************************************************************************************************************//
    //                                       4: Lock Methods(16)                                                      //
    //****************************************************************************************************************//
    public boolean isFair() {
        return waitPool.isFair();
    }

    private Thread getOwner() {
        return writerLock.getOwner();
    }

    public int getReadLockCount() {
        return sharedCount(lockState.get());
    }

    public int getWriteLockCount() {
        return exclusiveCount(lockState.get());
    }

    public boolean isWriteLocked() {
        return writerLock.isLocked();
    }

    public boolean isWriteLockedByCurrentThread() {
        return writerLock.getOwner() == Thread.currentThread();
    }

    public int getWriteHoldCount() {
        return writerLock.getHoldCount();
    }

    public int getReadHoldCount() {
        return readerLock.getHoldCount();
    }

    private Collection<Thread> getQueuedWriterThreads() {
        return writerLock.getQueuedThreads();
    }

    private Collection<Thread> getQueuedReaderThreads() {
        return waitPool.getQueuedThreads();
    }

    public boolean hasQueuedThreads() {
        return waitPool.hasQueuedThreads();
    }

    public boolean hasQueuedThread(Thread thread) {
        return waitPool.hasQueuedThread(thread);
    }

    public int getQueueLength() {
        return waitPool.getQueueLength();
    }

    private Collection<Thread> getQueuedThreads() {
        return waitPool.getQueuedThreads();
    }

    public boolean hasWaiters(Condition condition) {
        return getLockConditionImpl(condition).hasWaiters();
    }

    public int getWaitQueueLength(Condition condition) {
        return getLockConditionImpl(condition).getWaitQueueLength();
    }

    private Collection<Thread> getWaitingThreads(Condition condition) {
        return getLockConditionImpl(condition).getWaitingThreads();
    }

    public String toString() {
        int c = lockState.get();
        int w = exclusiveCount(c);
        int r = sharedCount(c);
        return super.toString() +
                "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    //****************************************************************************************************************//
    //                                      4: WriteLock/ReadLock Impl                                                //                                                                                  //
    //****************************************************************************************************************//
    public static class WriteLock extends BaseLock {
        WriteLock(ResultWaitPool waitPool, LockAction lockAction) {
            super(waitPool, lockAction, SyncConstants.TYPE_EXCLUSIVE);
        }

        public int getHoldCount() {
            return lockAction.getHoldCount();
        }
    }

    public static class ReadLock extends BaseLock {
        ReadLock(ResultWaitPool waitPool, LockAction lockAction) {
            super(waitPool, lockAction, SyncConstants.TYPE_SHARED);
        }

        public int getHoldCount() {
            return lockAction.getHoldCount();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    //****************************************************************************************************************//
    //                                       5: Write Lock Action Impl                                                //
    //****************************************************************************************************************//
    private static class WriteLockAction extends LockAction {
        WriteLockAction(LockAtomicState lockState) {
            super(lockState);
        }

        public int getHoldCount() {
            return lockState.exclusiveOwnerThread == Thread.currentThread() ? exclusiveCount(lockState.get()) : 0;
        }

        public Object call(Object size) {
            int state = lockState.get();
            if (state == 0) {
                state = incrementExclusivePart(state, (int) size);
                if (lockState.compareAndSet(0, state)) {
                    lockState.exclusiveOwnerThread = Thread.currentThread();
                    return true;
                } else {
                    return false;
                }
            } else if (lockState.exclusiveOwnerThread == Thread.currentThread()) {//Reentrant
                state = incrementExclusivePart(state, (int) size);
                if (exclusiveCount(state) > MAX_COUNT) throw new Error("Maximum lock count exceeded");
                lockState.set(state);
                return true;
            } else {
                return false;
            }
        }

        public boolean tryRelease(int size) {
            if (lockState.exclusiveOwnerThread == Thread.currentThread()) {
                int curState = lockState.get();
                int writeCount = exclusiveCount(curState);
                writeCount = writeCount - size;//support full release for reentrant

                lockState.set(decrementExclusivePart(curState, size));
                if (writeCount == 0) lockState.exclusiveOwnerThread = null;

                return writeCount == 0;
            } else {
                return false;
            }
        }
    }

    //****************************************************************************************************************//
    //                                       6: Read Lock Action Impl                                                 //
    //****************************************************************************************************************//
    private static class SharedHoldCounter {
        private int holdCount = 0;
    }

    private static class ReadLockAction extends LockAction {
        //cache shared hold count
        private final ThreadLocal<SharedHoldCounter> sharedHoldThreadLocal;

        ReadLockAction(LockAtomicState lockState) {
            super(lockState);
            this.sharedHoldThreadLocal = new ThreadLocal<>();
        }

        public int getHoldCount() {
            SharedHoldCounter counter = sharedHoldThreadLocal.get();
            return counter != null ? counter.holdCount : 0;
        }

        public Object call(Object size) {
            int state = lockState.get();
            int writeCount = exclusiveCount(state);

            //step1:test current is whether in exclusive mode
            if (writeCount > 0) {
                if (lockState.exclusiveOwnerThread == Thread.currentThread()) {
                    state = incrementSharedPart(state, (int) size);//+1
                    if (sharedCount(state) > MAX_COUNT) throw new Error("Maximum lock count exceeded");

                    lockState.set(state);
                    SharedHoldCounter holdCounter = sharedHoldThreadLocal.get();
                    if (holdCounter == null) {
                        holdCounter = new SharedHoldCounter();
                        sharedHoldThreadLocal.set(holdCounter);
                    }

                    holdCounter.holdCount++;
                    return true;
                } else {
                    return false;
                }
            }

            //step2:try to cas new state(share mode)
            int newState = incrementSharedPart(state, (int) size);
            int sharedCount = sharedCount(newState);
            if (sharedCount > MAX_COUNT) throw new Error("Maximum lock count exceeded");
            if (lockState.compareAndSet(state, newState)) {
                SharedHoldCounter holdCounter = sharedHoldThreadLocal.get();
                if (holdCounter == null) {
                    holdCounter = new SharedHoldCounter();
                    sharedHoldThreadLocal.set(holdCounter);
                }
                holdCounter.holdCount++;

                return true;
            }
            return false;
        }

        public boolean tryRelease(int size) {
            SharedHoldCounter holdCounter = sharedHoldThreadLocal.get();
            if (holdCounter == null) return false;
            holdCounter.holdCount -= size;
            if (holdCounter.holdCount <= 0) sharedHoldThreadLocal.remove();

            int state, updState, readCount;
            do {
                state = lockState.get();
                readCount = sharedCount(state);
                if (readCount == 0) return false;
                updState = decrementSharedPart(state, size);
                if (lockState.compareAndSet(state, updState))
                    return true;
            } while (true);
        }
    }
}
