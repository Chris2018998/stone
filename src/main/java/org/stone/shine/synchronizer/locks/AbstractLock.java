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
import org.stone.shine.synchronizer.extend.ResourceAction;
import org.stone.shine.synchronizer.extend.ResourceWaitPool;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Lock super class(ReentrantLock,WriteLock,ReadLock)
 *
 * @author Chris Liao
 * @version 1.0
 */

abstract class AbstractLock implements Lock {
    //Exclusive acquisition type(set to wait node value)
    static final Object Exclusive = new Object();
    //Sharable acquisition type(set to wait node value)
    static final Object Sharable = new Object();

    //Lock Acquire Action(ReentrantAction,WriteLockAction,ReadLockAction)
    private final ResourceAction lockAction;
    //resource wait Pool
    private final ResourceWaitPool resourceWaitPool;

    //constructor
    public AbstractLock(boolean fair, ResourceAction lockAction) {
        this.lockAction = lockAction;
        this.resourceWaitPool = new ResourceWaitPool(fair);
    }

    //constructor
    public AbstractLock(ResourceAction lockAction, ResourceWaitPool resourceWaitPool) {
        this.lockAction = lockAction;
        this.resourceWaitPool = resourceWaitPool;
    }

    //****************************************************************************************************************//
    //                                          1: monitor Methods(5)                                                 //
    //****************************************************************************************************************//
    public final boolean isFair() {
        return resourceWaitPool.isFair();
    }

    public final int getQueueLength() {
        return resourceWaitPool.getQueueLength();
    }

    public final boolean hasQueuedThreads() {
        return resourceWaitPool.hasQueuedThreads();
    }

    public final Collection<Thread> getQueuedThreads() {
        return resourceWaitPool.getQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return resourceWaitPool.hasQueuedThread(thread);
    }

    public abstract Thread getHoldThread();

    //****************************************************************************************************************//
    //                                          2: lock methods                                                       //
    //****************************************************************************************************************//
    public void lock() {
        try {
            ThreadParkSupport parker = ThreadParkSupport.create();
            resourceWaitPool.acquire(lockAction, 1, parker, false, null, true);
        } catch (Exception e) {
            //do nothing
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        ThreadParkSupport parker = ThreadParkSupport.create();
        resourceWaitPool.acquire(lockAction, 1, parker, true, null, true);
    }

    public boolean tryLock() {
        return resourceWaitPool.tryAcquire(lockAction, 1);
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (unit == null) throw new IllegalArgumentException("time unit can't be null");
        ThreadParkSupport parker = ThreadParkSupport.create(unit.toNanos(time), false);
        return resourceWaitPool.acquire(lockAction, 1, parker, true, null, true);
    }

    public void unlock() {
        resourceWaitPool.release(lockAction, 1);
    }

    public Condition newCondition() {
        return new LockConditionImpl(this);
    }

    //****************************************************************************************************************//
    //                                        3: Lock Condition Impl                                                  //                                                                                  //
    //****************************************************************************************************************//
    private static class LockConditionImpl extends SignalWaitPool implements Condition {
        private AbstractLock lock;

        LockConditionImpl(AbstractLock lock) {
            this.lock = lock;
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
            ThreadNode conditionNode = super.createNode(Exclusive);//condition just support Exclusive mode
            try {
                //occurred InterruptedException,just caught it and not send the wakeup-signal to other waiter
                super.doWait(support, throwsIE, conditionNode, false);
            } catch (InterruptedException e) {
                waitInterruptedException = e;
            }

            //3:reacquire the single PermitPool with exclusive mode and ignore interruption(must get success)
            conditionNode.setState(null);//reset to null(need filled by other)
            lock.resourceWaitPool.acquire(lock.lockAction, 1, ThreadParkSupport.create(), false, conditionNode, false);

            //4:throw occurred interrupt exception on condition wait
            if (waitInterruptedException != null) throw waitInterruptedException;

            /**
             * my individual view:throw InterruptedException may be a better schema at step2 (different to professor Doug Lea)
             * 1: not need't join in syn queue to get lock
             * 2: it add chance to take the lock for other threads
             */
        }

        public void signal() {
            if (lock.getHoldThread() != Thread.currentThread()) throw new IllegalMonitorStateException();
            super.wakeupOne();//node wait(step2) in the doAwait method
        }

        public void signalAll() {
            if (lock.getHoldThread() != Thread.currentThread()) throw new IllegalMonitorStateException();
            super.wakeupAll();//node wait(step2) in the doAwait method
        }
    }
}


