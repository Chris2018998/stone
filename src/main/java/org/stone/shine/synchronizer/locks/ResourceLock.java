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
import org.stone.shine.synchronizer.base.ResultCall;
import org.stone.shine.synchronizer.base.ResultWaitPool;
import org.stone.shine.synchronizer.base.SignalWaitPool;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

/**
 * A resource lock synchronizer implementation,whose instance can be regards as single PermitPool.
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ResourceLock extends ResultWaitPool {
    //Exclusive acquisition type(set to wait node value)
    private static final Object Exclusive = new Object();
    //Sharable acquisition type(set to wait node value)
    private static final Object Sharable = new Object();

    //lock PermitPool hold state(0:not held,1:first held,greater than 1:reentrant count)
    //reentrant:hold count of an exclusive thread,or total hold count of all threads under sharable hold mode
    private final AtomicInteger state = new AtomicInteger(0);
    //trace sharable hold count of lock threads(support reentrant of a thread in sharable hold mode)
    private final ThreadLocal<SharedHoldCounter> sharableHoldInfo = new SharedHoldThreadLocal();

    //sharable acquire action(drove by result wait pool)
    private final LockAction sharableLockAction = new SharableLockAction(this);
    //exclusive acquire action(drove by result wait pool)
    private final LockAction exclusiveLockAction = new ExclusiveLockAction(this);

    //current hold type(Exclusive or Sharable,@see static definition,first row and second row in this file body)
    private Object currentHoldType;
    //hold thread(an exclusive thread or first thread success acquired under sharable mode)
    private Thread currentHoldThread;

    //****************************************************************************************************************//
    //                                          1: constructor(2)                                                     //
    //****************************************************************************************************************//
    public ResourceLock() {
        this(false);
    }

    public ResourceLock(boolean fair) {
        super(fair);
    }

    //****************************************************************************************************************//
    //                                          2: acquire/release(6)                                                 //
    //****************************************************************************************************************//
    //2.1:release exclusive PermitPool to pool
    public void release() {
        exclusiveLockAction.release();
    }

    //2.2:try to acquire as exclusive PermitPool
    public boolean tryAcquire() {
        return exclusiveLockAction.tryReentrant() || exclusiveLockAction.tryAcquire();
    }

    //2.3: acquire with exclusive mode
    public boolean acquire(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        return exclusiveLockAction.tryReentrant() || acquireByAction(exclusiveLockAction, parker, throwsIE, Exclusive);
    }

    //2.4: create a new lock condition(just support exclusive mode)
    public Condition newCondition() {
        if (this.isExclusiveHeldByCurrentThread())
            return new LockConditionImpl(this);
        else
            throw new IllegalMonitorStateException();
    }

    //2.5: acquire as exclusive PermitPool for condition node
    private void acquireForConditionNode(ThreadParkSupport support, ThreadNode conditionNode) {
        try {
            super.doCallForNode(exclusiveLockAction, 1, true, support, false, conditionNode, false);
        } catch (Exception e) {
            //do nothing
        }
    }

    //2.6: acquisition core drove method by result wait pool
    private boolean acquireByAction(LockAction action, ThreadParkSupport support, boolean throwsIE, Object acquisitionType) throws InterruptedException {
        try {
            return super.doCall(action, 1, true, support, throwsIE, acquisitionType);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            //this exception caught just fit super's method invocation
            //in fact,only InterruptedException can be thrown out,so return false;
            return false;
        }
    }

    //****************************************************************************************************************//
    //                                          3: acquire shareable/release(3)                                       //
    //****************************************************************************************************************//
    //3.1:release with sharable mode
    public void releaseShared() {
        sharableLockAction.release();
    }

    //3.2:try to acquire with sharable mode
    public boolean tryAcquireShared() {
        return sharableLockAction.tryReentrant() || sharableLockAction.tryAcquire();
    }

    //3.3: acquire with sharable mode
    public boolean acquireShared(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        return sharableLockAction.tryReentrant() || acquireByAction(sharableLockAction, parker, throwsIE, Sharable);
    }

    //****************************************************************************************************************//
    //                                          4: monitor methods(8)                                                 //
    //****************************************************************************************************************//
    public boolean isHeld() {
        return state.get() > 0;
    }

    public boolean isExclusiveHeld() {
        return currentHoldType == Exclusive && state.get() > 0;
    }

    public boolean isSharableHeld() {
        return currentHoldType == Sharable && state.get() > 0;
    }

    public Thread getHeldThread() {
        return this.currentHoldThread;
    }

    public boolean isHeldByCurrentThread() {
        return isExclusiveHeldByCurrentThread() || isSharableHeldByCurrentThread();
    }

    public boolean isExclusiveHeldByCurrentThread() {
        return isExclusiveHeld() && currentHoldThread == Thread.currentThread();
    }

    public boolean isSharableHeldByCurrentThread() {
        SharedHoldCounter holdInfo = sharableHoldInfo.get();
        return isSharableHeld() && holdInfo != null && holdInfo.holdCount > 0;
    }

    public int getHoldCountByCurrentThread() {
        if (isExclusiveHeldByCurrentThread()) {
            return state.get();
        } else if (isSharableHeldByCurrentThread()) {
            SharedHoldCounter holdInfo = sharableHoldInfo.get();
            return holdInfo != null ? holdInfo.holdCount : 0;
        } else {
            return 0;
        }
    }

    //****************************************************************************************************************//
    //                                        5: Lock Condition Impl                                                  //                                                                                  //
    //****************************************************************************************************************//
    private static class LockConditionImpl extends SignalWaitPool implements Condition {
        private ResourceLock lock;

        LockConditionImpl(ResourceLock lock) {
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
            this.lock.release();

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
            lock.acquireForConditionNode(ThreadParkSupport.create(0, false), conditionNode);

            //4:throw occurred interrupt exception on condition wait
            if (waitInterruptedException != null) throw waitInterruptedException;

            /**
             * my individual view:throw InterruptedException may be a better schema at step2 (different to professor Doug Lea)
             * 1: not need't join in syn queue to get lock
             * 2: it add chance to take the lock for other threads
             */
        }

        public void signal() {
            if (!lock.isExclusiveHeldByCurrentThread()) throw new IllegalMonitorStateException();
            super.wakeupOne();//node wait(step2) in the doAwait method
        }

        public void signalAll() {
            if (!lock.isExclusiveHeldByCurrentThread()) throw new IllegalMonitorStateException();
            super.wakeupAll();//node wait(step2) in the doAwait method
        }
    }

    //****************************************************************************************************************//
    //                                        6: Lock Action define(5)                                                //                                                                                  //
    //****************************************************************************************************************//
    //6.1:lock action abstract class(drove plugin by result call pool)
    private static abstract class LockAction implements ResultCall {
        protected ResourceLock lock;
        protected AtomicInteger state;

        public LockAction(ResourceLock lock) {
            this.lock = lock;
            this.state = lock.state;
        }

        //release lock hold
        abstract void release();

        //try to reentrant lock under held
        abstract boolean tryReentrant();

        //try to acquire lock
        boolean tryAcquire() {
            try {
                return Objects.equals(this.call(1), true);
            } catch (Exception e) {
                return false;
            }
        }
    }

    //6.2: Exclusive lock action Implementation
    private static class ExclusiveLockAction extends LockAction {
        ExclusiveLockAction(ResourceLock access) {
            super(access);
        }

        //6.2.1: acquire lock(drove by result pool)
        public Object call(Object arg) {
            if (state.compareAndSet(0, 1)) {
                lock.currentHoldThread = Thread.currentThread();
                lock.currentHoldType = Exclusive;
                return true;
            } else {
                return false;
            }
        }

        //6.2.2: try reentrant acquire
        public boolean tryReentrant() {
            if (lock.isExclusiveHeldByCurrentThread()) {
                int c = state.get();
                c++;

                if (c <= 0) throw new Error("Maximum lock count exceeded");
                state.set(c);//because the lock held by current thread,so need't cas state
                return true;
            } else {
                return false;
            }
        }

        //6.2.3: release lock
        public void release() {
            if (lock.isExclusiveHeldByCurrentThread()) {
                int c;
                do {
                    c = state.get();
                    if (c > 0) {
                        c = c - 1;
                        if (c == 0) {
                            lock.currentHoldType = null;
                            lock.currentHoldThread = null;

                            state.set(c);
                            lock.wakeupOne();//the wakeup maybe a sharable waiter or an exclusive waiter
                        }
                    } else {
                        return;
                    }
                } while (true);
            } else {//if support interruptException thrown out condition await,the 'else' block should be disabled
                throw new IllegalMonitorStateException();
            }
        }
    }

    //6.3:hold lock count of a thread in sharable mode
    private static class SharedHoldCounter {
        private int holdCount = 0;
    }

    //6.4:store sharable hold count of threads
    private static class SharedHoldThreadLocal extends ThreadLocal<SharedHoldCounter> {
        protected SharedHoldCounter initialValue() {
            return new SharedHoldCounter();
        }
    }

    //6.5: Sharable lock action Implementation
    private static class SharableLockAction extends LockAction {
        SharableLockAction(ResourceLock access) {
            super(access);
        }

        public Object call(Object arg) {
            return true;
        }

        public boolean tryReentrant() {
            return true;
        }

        public void release() {

        }
    }
}
