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

import org.stone.shine.synchronizer.ThreadNode;
import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.base.SignalWaitPool;
import org.stone.shine.synchronizer.extend.AcquireTypes;
import org.stone.shine.synchronizer.extend.ResourceWaitPool;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Lock super class(ReentrantLock,WriteLockImpl,ReadLockImpl)
 *
 * @author Chris Liao
 * @version 1.0
 */

class BaseLock implements Lock {
    //Lock State
    protected final LockAtomicState lockState;
    //resource acquire type
    private final Object acquireType;
    //Lock Acquire Action(ReentrantLockAction,WriteLockAction,ReadLockAction)
    private final LockAction lockAction;
    //resource wait Pool
    private final ResourceWaitPool waitPool;

    //****************************************************************************************************************//
    //                                          1: constructors (2)                                                   //
    //****************************************************************************************************************//
    //constructor1(extend by ReentrantLock)
    BaseLock(boolean fair, LockAction lockAction) {
        this(new ResourceWaitPool(fair), lockAction, AcquireTypes.TYPE_Exclusive);
    }

    //constructor2(extend by WriteLockImpl,ReadLockImpl)
    BaseLock(ResourceWaitPool waitPool, LockAction lockAction, Object acquireType) {
        this.waitPool = waitPool;
        this.lockAction = lockAction;
        this.acquireType = acquireType;
        this.lockState = lockAction.getLockState();
    }

    //****************************************************************************************************************//
    //                                          2: lock methods (6)                                                   //
    //****************************************************************************************************************//

    /**
     * Acquires the lock.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation may be able to detect erroneous use
     * of the lock, such as an invocation that would cause deadlock, and
     * may throw an (unchecked) exception in such circumstances.  The
     * circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     */
    public void lock() {
        try {
            ThreadParkSupport parker = ThreadParkSupport.create();
            waitPool.acquireWithType(lockAction, 1, parker, false, acquireType, true);
        } catch (Exception e) {
            //do nothing
        }
    }

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is available and returns immediately.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     *
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the
     * lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some
     * implementations may not be possible, and if possible may be an
     * expensive operation.  The programmer should be aware that this
     * may be the case. An implementation should document when this is
     * the case.
     *
     * <p>An implementation can favor responding to an interrupt over
     * normal method return.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would
     * cause deadlock, and may throw an (unchecked) exception in such
     * circumstances.  The circumstances and the exception type must
     * be documented by that {@code Lock} implementation.
     *
     * @throws InterruptedException if the current thread is
     *                              interrupted while acquiring the lock (and interruption
     *                              of lock acquisition is supported)
     */
    public void lockInterruptibly() throws InterruptedException {
        ThreadParkSupport parker = ThreadParkSupport.create();
        waitPool.acquireWithType(lockAction, 1, parker, true, acquireType, true);
    }

    /**
     * Acquires the lock only if it is free at the time of invocation.
     *
     * <p>Acquires the lock if it is available and returns immediately
     * with the value {@code true}.
     * If the lock is not available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>A typical usage idiom for this method would be:
     * <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     * <p>
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     *
     * @return {@code true} if the lock was acquired and
     * {@code false} otherwise
     */
    public boolean tryLock() {
        return waitPool.tryAcquire(lockAction, 1);
    }

    /**
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the lock is available this method returns immediately
     * with the value {@code true}.
     * If the lock is not available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported; or
     * <li>The specified waiting time elapses
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.
     * If the time is
     * less than or equal to zero, the method will not wait at all.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some implementations
     * may not be possible, and if possible may
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return, or reporting a timeout.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would cause
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     * if the waiting time elapsed before the lock was acquired
     * @throws InterruptedException if the current thread is interrupted
     *                              while acquiring the lock (and interruption of lock
     *                              acquisition is supported)
     */
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (unit == null) throw new IllegalArgumentException("time unit can't be null");
        ThreadParkSupport parker = ThreadParkSupport.create(unit.toNanos(time), false);
        return waitPool.acquireWithType(lockAction, 1, parker, true, acquireType, true);
    }

    /**
     * Releases the lock.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     */
    public void unlock() {
        waitPool.release(lockAction, 1);
    }

    /**
     * Returns a new {@link Condition} instance that is bound to this
     * {@code Lock} instance.
     *
     * <p>Before waiting on the condition the lock must be held by the
     * current thread.
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquireWithType the lock before the wait returns.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The exact operation of the {@link Condition} instance depends on
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *                                       implementation does not support conditions
     */
    public Condition newCondition() {
        return new LockConditionImpl(this);
    }

    //****************************************************************************************************************//
    //                                          3: monitor Methods(5)                                                 //
    //****************************************************************************************************************//
    public final boolean isFair() {
        return waitPool.isFair();
    }

    public final int getQueueLength() {
        return waitPool.getQueueLength();
    }

    public final boolean hasQueuedThreads() {
        return waitPool.hasQueuedThreads();
    }

    public final Collection<Thread> getQueuedThreads() {
        return waitPool.getQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return waitPool.hasQueuedThread(thread);
    }

    //****************************************************************************************************************//
    //                                          4: Condition Methods(4)                                               //
    //****************************************************************************************************************//
    public boolean hasWaiters(Condition condition) {
        return castConditionToLocal(condition).hasQueuedThreads();
    }

    public int getWaitQueueLength(Condition condition) {
        return castConditionToLocal(condition).getQueueLength();
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        return castConditionToLocal(condition).getQueuedThreads();
    }

    private LockConditionImpl castConditionToLocal(Condition condition) {
        if (condition == null) throw new NullPointerException();
        if (!(condition instanceof LockConditionImpl)) throw new IllegalArgumentException("not owner");
        return (LockConditionImpl) condition;
    }

    //****************************************************************************************************************//
    //                                       5: Lock Condition Impl                                                  //                                                                                  //
    //****************************************************************************************************************//
    private static class LockConditionImpl extends SignalWaitPool implements Condition {
        private final BaseLock lock;
        private final LockAction lockAction;

        LockConditionImpl(BaseLock lock) {
            this.lock = lock;
            this.lockAction = lock.lockAction;
        }

        public void await() throws InterruptedException {
            this.doAwait(ThreadParkSupport.create(0, false), true);
        }

        public void awaitUninterruptibly() {
            try {
                this.doAwait(ThreadParkSupport.create(0, false), false);
            } catch (InterruptedException e) {
                //in fact,InterruptedException never throws here
            }
        }

        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            ThreadParkSupport support = ThreadParkSupport.create(nanosTimeout, false);
            this.doAwait(support, true);
            return support.getParkTime();
        }

        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            if (unit == null) throw new IllegalArgumentException("time unit can't be null");
            ThreadParkSupport support = ThreadParkSupport.create(unit.toNanos(time), false);
            this.doAwait(support, true);
            return support.isTimeout();
        }

        public boolean awaitUntil(Date deadline) throws InterruptedException {
            if (deadline == null) throw new IllegalArgumentException("dead line can't be null");
            ThreadParkSupport support = ThreadParkSupport.create(deadline.getTime(), true);
            this.doAwait(support, true);
            return support.isTimeout();
        }

        //do await
        private void doAwait(ThreadParkSupport support, boolean throwsIE) throws InterruptedException {
            //1:release the single PermitPool under exclusive mode to pool
            this.lock.unlock();

            //2:join in the condition queue and wait a wakeup-signal from other
            InterruptedException waitInterruptedException = null;
            ThreadNode conditionNode = super.createNode(AcquireTypes.TYPE_Exclusive);//condition just support Exclusive mode
            try {
                //occurred InterruptedException,just caught it and not send the wakeup-signal to other waiter
                super.doWait(support, throwsIE, conditionNode, false);
            } catch (InterruptedException e) {
                waitInterruptedException = e;
            }

            //3:reacquire the single PermitPool with exclusive mode and ignore interruption(must get success)
            conditionNode.setState(null);//reset to null(need filled by other)
            lock.waitPool.acquireWithNode(lockAction, 1, ThreadParkSupport.create(), false, conditionNode, false);

            //4:throw occurred interrupt exception on condition wait
            if (waitInterruptedException != null) throw waitInterruptedException;

            /**
             * my individual view:throw InterruptedException may be a better schema at step2 (different to professor Doug Lea)
             * 1: not need't join in syn queue to get lock
             * 2: it add chance to take the lock for other threads
             */
        }

        public void signal() {
            if (lockAction.getHoldThread() != Thread.currentThread()) throw new IllegalMonitorStateException();
            super.wakeupOne();//node wait(step2) in the doAwait method
        }

        public void signalAll() {
            if (lockAction.getHoldThread() != Thread.currentThread()) throw new IllegalMonitorStateException();
            super.wakeupAll();//node wait(step2) in the doAwait method
        }
    }
}


