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

import org.stone.shine.synchronizer.base.ResultWaitPool;
import org.stone.shine.synchronizer.extend.AtomicLongState;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Stamped Lock Implementation By Wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class StampedLock implements java.io.Serializable {
    private static final int LG_READERS = 7;
    private static final long RUNIT = 1L;
    private static final long WBIT = 1L << LG_READERS;
    private static final long RBITS = WBIT - 1L;
    private static final long RFULL = RBITS - 1L;
    private static final long ABITS = RBITS | WBIT;
    private static final long SBITS = ~RBITS; // note overlap with ABITS
    //Initial value for lock state; avoid failure value zero
    private static final long ORIGIN = WBIT << 1;

    private final ResultWaitPool waitPool;
    private final AtomicLongState lockState;

    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;

    public StampedLock() {
        this.waitPool = new ResultWaitPool();
        this.lockState = new AtomicLongState(ORIGIN);
    }

    //****************************************************************************************************************//
    //                                          1: Write methods (4)                                                  //
    //****************************************************************************************************************//

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    public long writeLock() {
        return 1;
    }

    /**
     * Exclusively acquires the lock if it is immediately available.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryWriteLock() {
        return 1;
    }

    /**
     * Exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link java.util.concurrent.locks.Lock#tryLock(long, TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    public long tryWriteLock(long time, TimeUnit unit) {
        return 1;
    }

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link java.util.concurrent.locks.Lock#lockInterruptibly()}.
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    public long writeLockInterruptibly() throws InterruptedException {
        return 1;
    }

    /**
     * If the lock state matches the given stamp, releases the
     * exclusive lock.
     *
     * @param stamp a stamp returned by a write-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     *                                      not isExpect the current state of this lock
     */
    public void unlockWrite(long stamp) {

    }

    public boolean validate(long stamp) {
        return true;
    }


    public boolean tryUnlockWrite() {
        return true;
    }

    public boolean isWriteLocked() {
        return true;
    }

    //****************************************************************************************************************//
    //                                          2: Write methods (4)                                                  //
    //****************************************************************************************************************//

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    public long readLock() {
        return 1;
    }


    /**
     * Non-exclusively acquires the lock if it is immediately available.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryReadLock() {
        return 1;
    }

    /**
     * Non-exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link java.util.concurrent.locks.Lock#tryLock(long, TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    public long tryReadLock(long time, TimeUnit unit) {
        return 1;
    }

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link java.util.concurrent.locks.Lock#lockInterruptibly()}.
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    public long readLockInterruptibly() throws InterruptedException {
        return 1;
    }

    /**
     * If the lock state matches the given stamp, releases the
     * non-exclusive lock.
     *
     * @param stamp a stamp returned by a read-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     *                                      not isExpect the current state of this lock
     */
    public void unlockRead(long stamp) {
    }

    public boolean tryUnlockRead() {
        return true;
    }

    public int getReadLockCount() {
        return 1;
    }

    private int getReadLockCount(long s) {
        return 1;
    }

    public boolean isReadLocked() {
        return true;
    }


    //****************************************************************************************************************//
    //                                          2:Lock View                                                           //
    //****************************************************************************************************************//

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #readLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link
     * Lock#newCondition()} throws {@code
     * UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asReadLock() {
        ReadLockView v;
        return ((v = readLockView) != null ? v :
                (readLockView = new ReadLockView()));
    }

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #writeLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link
     * Lock#newCondition()} throws {@code
     * UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asWriteLock() {
        WriteLockView v;
        return ((v = writeLockView) != null ? v :
                (writeLockView = new WriteLockView()));
    }

    /**
     * Returns a {@link ReadWriteLock} view of this StampedLock in
     * which the {@link ReadWriteLock#readLock()} method is mapped to
     * {@link #asReadLock()}, and {@link ReadWriteLock#writeLock()} to
     * {@link #asWriteLock()}.
     *
     * @return the lock
     */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        return ((v = readWriteLockView) != null ? v :
                (readWriteLockView = new ReadWriteLockView()));
    }

    // view classes
    final class ReadLockView implements Lock {
        public void lock() {
            readLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }

        public boolean tryLock() {
            return tryReadLock() != 0L;
        }

        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }

        public void unlock() {
            //unstampedUnlockRead();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class WriteLockView implements Lock {
        public void lock() {
            writeLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }

        public boolean tryLock() {
            return tryWriteLock() != 0L;
        }

        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }

        public void unlock() {
            //unstampedUnlockWrite();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        public Lock readLock() {
            return asReadLock();
        }

        public Lock writeLock() {
            return asWriteLock();
        }
    }
}
