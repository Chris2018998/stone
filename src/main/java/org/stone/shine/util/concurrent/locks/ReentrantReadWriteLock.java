/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.locks;

import org.stone.shine.util.concurrent.synchronizer.extend.AcquireTypes;
import org.stone.shine.util.concurrent.synchronizer.extend.ResourceWaitPool;

import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;

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
    private final ResourceWaitPool waitPool;
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
        this.waitPool = new ResourceWaitPool(fair);
        this.writerLock = new WriteLock(waitPool, new WriteLockAction(lockState));
        this.readerLock = new ReadLock(waitPool, new ReadLockAction(lockState, waitPool));
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
    public final boolean isFair() {
        return waitPool.isFair();
    }

    protected Thread getOwner() {
        return writerLock.getOwner();
    }

    public int getReadLockCount() {
        return sharedCount(lockState.getState());
    }

    public int getWriteLockCount() {
        return exclusiveCount(lockState.getState());
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

    protected Collection<Thread> getQueuedWriterThreads() {
        return writerLock.getQueuedThreads();
    }

    protected Collection<Thread> getQueuedReaderThreads() {
        return waitPool.getQueuedThreads();
    }

    public final boolean hasQueuedThreads() {
        return waitPool.hasQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return waitPool.hasQueuedThread(thread);
    }

    public final int getQueueLength() {
        return waitPool.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return waitPool.getQueuedThreads();
    }

    public boolean hasWaiters(Condition condition) {
        return getLockConditionImpl(condition).hasWaiters();
    }

    public int getWaitQueueLength(Condition condition) {
        return getLockConditionImpl(condition).getWaitQueueLength();
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        return getLockConditionImpl(condition).getWaitingThreads();
    }

    public String toString() {
        int c = lockState.getState();
        int w = exclusiveCount(c);
        int r = sharedCount(c);
        return super.toString() +
                "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    //****************************************************************************************************************//
    //                                      4: WriteLock/ReadLock Impl                                                //                                                                                  //
    //****************************************************************************************************************//
    public static class WriteLock extends BaseLock {
        WriteLock(ResourceWaitPool waitPool, LockAction lockAction) {
            super(waitPool, lockAction, AcquireTypes.TYPE_EXCLUSIVE);
        }

        public int getHoldCount() {
            return lockAction.getHoldCount();
        }
    }

    public static class ReadLock extends BaseLock {
        ReadLock(ResourceWaitPool waitPool, LockAction lockAction) {
            super(waitPool, lockAction, AcquireTypes.TYPE_SHARED);
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
            return lockState.getExclusiveOwnerThread() == Thread.currentThread() ? exclusiveCount(lockState.getState()) : 0;
        }

        public Object call(Object size) {
            int state = lockState.getState();
            if (state == 0) {
                state = incrementExclusivePart(state, (int) size);
                if (lockState.compareAndSetState(0, state)) {
                    lockState.setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                } else {
                    return false;
                }
            } else if (lockState.getExclusiveOwnerThread() == Thread.currentThread()) {//Reentrant
                state = incrementExclusivePart(state, (int) size);
                if (exclusiveCount(state) > MAX_COUNT) throw new Error("Maximum lock count exceeded");
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
                writeCount = writeCount - size;//support full release for reentrant

                lockState.setState(decrementExclusivePart(curState, size));
                if (writeCount == 0) lockState.setExclusiveOwnerThread(null);

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
        private final ResourceWaitPool waitPool;
        //cache shared hold count
        private final ThreadLocal<SharedHoldCounter> sharedHoldThreadLocal;

        ReadLockAction(LockAtomicState lockState, ResourceWaitPool waitPool) {
            super(lockState);
            this.waitPool = waitPool;
            this.sharedHoldThreadLocal = new ThreadLocal<>();
        }

        public int getHoldCount() {
            SharedHoldCounter counter = sharedHoldThreadLocal.get();
            return counter != null ? counter.holdCount : 0;
        }

        public Object call(Object size) {
            int state = lockState.getState();
            int writeCount = exclusiveCount(state);

            //step1:test current is whether in exclusive mode
            if (writeCount > 0) {
                if (Thread.currentThread() == lockState.getExclusiveOwnerThread()) {
                    state = incrementSharedPart(state, (int) size);//+1
                    if (sharedCount(state) > MAX_COUNT) throw new Error("Maximum lock count exceeded");

                    lockState.setState(state);
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
            if (lockState.compareAndSetState(state, newState)) {
                SharedHoldCounter holdCounter = sharedHoldThreadLocal.get();
                if (holdCounter == null) {
                    holdCounter = new SharedHoldCounter();
                    sharedHoldThreadLocal.set(holdCounter);
                }
                holdCounter.holdCount++;

                //first read head then wakeup others wait in share node
                if (sharedCount == 1) waitPool.wakeupOne(true, AcquireTypes.TYPE_SHARED, RUNNING);
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
                state = lockState.getState();
                readCount = sharedCount(state);
                if (readCount == 0) return false;
                updState = decrementSharedPart(state, size);
                if (lockState.compareAndSetState(state, updState))
                    return true;
            } while (true);
        }
    }
}
