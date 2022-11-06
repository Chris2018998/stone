/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.extend;

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
 * A resource access synchronizer implementation,whose instance can be regards as single permit.
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ResourceAccess extends ResultWaitPool {
    //Exclusive acquisition type(set to wait node value)
    private static final Object Exclusive = new Object();
    //Sharable acquisition type(set to wait node value)
    private static final Object Sharable = new Object();

    //sharable acquire action(drove in result wait pool to get access permit)
    private static final AcquireAction sharableAcquireAction = new SharableAcquireAction();
    //exclusive acquire action(drove in result wait pool)
    private static final AcquireAction exclusiveAcquireAction = new ExclusiveAcquireAction();

    //access permit hold state(0:not held,1:first held,greater than 1:reentrant count)
    //reentrant:hold count of an exclusive thread,or total hold count of all threads under sharable hold mode
    private final AtomicInteger state = new AtomicInteger(0);
    //trace sharable hold count of access threads(support reentrant of a thread in sharable hold mode)
    private final ThreadLocal<HoldCounter> sharableHoldInfo = new HoldThreadLocal();

    //current hold type(Exclusive or Sharable,@see static definition,first row and second row in this file body)
    private Object currentHoldType;
    //hold thread(an exclusive thread or first thread success acquired under sharable mode)
    private Thread currentHoldThread;

    //****************************************************************************************************************//
    //                                          1: Constructor(2)                                                     //
    //****************************************************************************************************************//
    public ResourceAccess() {
        this(false);
    }

    public ResourceAccess(boolean fair) {
        super(fair);
    }

    //****************************************************************************************************************//
    //                                          2: acquire/release(4)                                                 //
    //****************************************************************************************************************//
    //2.1:release exclusive permit to pool
    public void release() {
        exclusiveAcquireAction.release();
    }

    //2.2:try to acquire as exclusive permit(@todo need add reentrant logic before call exclusiveAcquireAction)
    public boolean tryAcquire() {
        return exclusiveAcquireAction.tryAcquire();
    }

    //2.3: acquire with exclusive mode(@todo need add reentrant logic before call acquireByAction method)
    public boolean acquire(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        return acquireByAction(exclusiveAcquireAction, parker, throwsIE, Exclusive);
    }

    //2.4: create a new lock condition(just support exclusive mode)
    public Condition newCondition() {
        if (this.isExclusiveHeldByCurrentThread())
            return new LockConditionImpl(this);
        else
            throw new IllegalMonitorStateException();
    }

    //2.5: acquire as exclusive permit for condition node
    private void acquireForConditionNode(ThreadParkSupport support, ThreadNode conditionNode) {
        try {
            super.doCallForNode(exclusiveAcquireAction, 1, true, support, false, conditionNode, false);
        } catch (Exception e) {
            //do nothing
        }
    }

    //2.6: acquisition core drove method by result wait pool
    private boolean acquireByAction(AcquireAction action, ThreadParkSupport support, boolean throwsIE, Object acquisitionType) throws InterruptedException {
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
    //                                          3: acquire shareable/release(4)                                       //
    //****************************************************************************************************************//
    //3.1:release with sharable mode
    public void releaseShared() {
        sharableAcquireAction.release();
    }

    //3.2:try to acquire with sharable mode(@todo need add reentrant logic before call sharableAcquireAction)
    public boolean tryAcquireShared() {
        return sharableAcquireAction.tryAcquire();
    }

    //3.3: acquire with sharable mode(@todo need add reentrant logic before call acquireByAction method)
    public boolean acquireShared(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        return acquireByAction(sharableAcquireAction, parker, throwsIE, Sharable);
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
        HoldCounter holdInfo = sharableHoldInfo.get();
        return isSharableHeld() && holdInfo != null && holdInfo.holdCount > 0;
    }

    public int getHoldCountByCurrentThread() {
        if (isExclusiveHeldByCurrentThread()) {
            return state.get();
        } else if (isSharableHeldByCurrentThread()) {
            HoldCounter holdInfo = sharableHoldInfo.get();
            return holdInfo != null ? holdInfo.holdCount : 0;
        } else {
            return 0;
        }
    }

    //****************************************************************************************************************//
    //                                  4:inner interface/class(2)                                                    //                                                                                  //
    //****************************************************************************************************************//
    private static class HoldCounter {
        private int holdCount = 0;
    }

    private static class HoldThreadLocal extends ThreadLocal<HoldCounter> {
        protected HoldCounter initialValue() {
            return new HoldCounter();
        }
    }

    private static abstract class AcquireAction implements ResultCall {
        abstract void release();

        public boolean tryAcquire() {
            try {
                return Objects.equals(this.call(1), true);
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static class SharableAcquireAction extends AcquireAction {

        public Object call(Object arg) {
            return true;
        }

        public void release() {

        }
    }

    private static class ExclusiveAcquireAction extends AcquireAction {
        public Object call(Object arg) {
            return true;
        }

        public void release() {

        }
    }

    //Lock Condition Implementation
    private static class LockConditionImpl extends SignalWaitPool implements Condition {
        private ResourceAccess access;

        LockConditionImpl(ResourceAccess access) {
            this.access = access;
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
            //1:release the single permit under exclusive mode to pool
            this.access.release();

            //2:join in the condition queue and wait a wakeup-signal from other
            InterruptedException waitInterruptedException = null;
            ThreadNode conditionNode = super.createNode(Exclusive);//condition just support Exclusive mode
            try {
                //occurred InterruptedException,just caught it and not send the wakeup-signal to other waiter
                super.doWait(support, throwsIE, conditionNode, false);
            } catch (InterruptedException e) {
                waitInterruptedException = e;
            }

            //3:reacquire the single permit with exclusive mode and ignore interruption(must get success)
            conditionNode.setState(null);//reset to null(need filled by other)
            access.acquireForConditionNode(ThreadParkSupport.create(0, false), conditionNode);

            //4:throw occurred interrupt exception on condition wait
            if (waitInterruptedException != null) throw waitInterruptedException;

            /**
             * my individual view:throw InterruptedException may be a better schema at step2 (different to professor Doug Lea)
             * 1: not need't join in syn queue to get lock
             * 2: it add chance to take the lock for other threads
             */
        }

        public void signal() {
            if (!access.isExclusiveHeldByCurrentThread()) throw new IllegalMonitorStateException();
            super.wakeupOne();//node wait(step2) in the doAwait method
        }

        public void signalAll() {
            if (!access.isExclusiveHeldByCurrentThread()) throw new IllegalMonitorStateException();
            super.wakeupAll();//node wait(step2) in the doAwait method
        }
    }
}
