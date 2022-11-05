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
 * A resource access synchronizer implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ResourceAccess extends ResultWaitPool {
    //Exclusive(set as acquire type of wait node)
    private static final Object Exclusive = new Object();
    //Sharable(set as acquire type of wait node)
    private static final Object Sharable = new Object();

    //sharable acquire action(a plugin used in result wait pool)
    private final AcquireAction sharableAcquireAction = new SharableAcquireAction();
    //exclusive acquire action(a plugin used in result wait pool)
    private final AcquireAction exclusiveAcquireAction = new ExclusiveAcquireAction();
    //reentrant count atomic(total count)
    private final AtomicInteger state = new AtomicInteger(0);//0 - no hold
    //thread local hold count(reentrant)
    private final ThreadLocal<HoldCounter> threadHoldTrace = new HoldThreadLocal();

    //current hold type
    private Object currentHoldType;
    //hold thread(exclusive thread or sharable first thread)
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
    //2.1:release with exclusive mode
    public void release() {
        exclusiveAcquireAction.release();
    }

    //2.2:try to acquire with exclusive mode
    public boolean tryAcquire() {
        return exclusiveAcquireAction.tryAcquire();
    }

    //2.3: acquire with exclusive mode
    public boolean acquire(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        return acquireByAction(exclusiveAcquireAction, parker, throwsIE);
    }

    //2.4: create a new lock condition
    public Condition newCondition() {
        if (currentHoldType == Exclusive && state.get() > 0) {
            if (!this.isExclusiveHeldByCurrentThread()) throw new IllegalMonitorStateException();
            return new LockConditionImpl(this);
        } else
            throw new IllegalMonitorStateException();
    }

    //****************************************************************************************************************//
    //                                          3: acquire shareable/release(4)                                       //
    //****************************************************************************************************************//
    //3.1:release with sharable mode
    public void releaseShared() {
        sharableAcquireAction.release();
    }

    //3.2:try to acquire with sharable mode
    public boolean tryAcquireShared() {
        return sharableAcquireAction.tryAcquire();
    }

    //3.3: acquire with sharable mode
    public boolean acquireShared(ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        return acquireByAction(sharableAcquireAction, parker, throwsIE);
    }

    //3.4: acquire implementation method by acquire action
    private boolean acquireByAction(AcquireAction action, ThreadParkSupport parker, boolean throwsIE) throws InterruptedException {
        try {
            super.doCall(action, 1, true, parker, throwsIE);
            return true;
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    //for condition acquire(@todo some need optimize <method>parkNodeThread</method>after thread interrupted)
    private boolean acquireByAction(AcquireAction action, ThreadParkSupport parker, boolean throwsIE, ThreadNode node) throws InterruptedException {
        try {
            super.doCall(action, 1, true, parker, throwsIE, node);
            return true;
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }


    //****************************************************************************************************************//
    //                                          4: monitor methods                                                    //
    //****************************************************************************************************************//
    public boolean isHeld() {
        return state.get() > 0;
    }

    public Thread getHeldThread() {
        return this.currentHoldThread;
    }

    //true,if hold with exclusive mode by current thread
    public boolean isExclusiveHeldByCurrentThread() {
        return currentHoldType == Exclusive && currentHoldThread == Thread.currentThread();
    }

    //true,if hold with shared mode by current thread
    public boolean isSharedHeldByCurrentThread() {
        HoldCounter holdInfo = threadHoldTrace.get();
        return !isExclusiveHeldByCurrentThread() && holdInfo != null && holdInfo.holdCount > 0;
    }

    //true,if hold by current thread
    public boolean isHeldByCurrentThread() {
        if (isExclusiveHeldByCurrentThread()) {
            return currentHoldThread == Thread.currentThread();
        } else {
            HoldCounter holdInfo = threadHoldTrace.get();
            return holdInfo != null && holdInfo.holdCount > 0;
        }
    }

    //return current thread hold count(reentrant)
    public int getHoldCountByCurrentThread() {
        if (isExclusiveHeldByCurrentThread()) {
            return state.get();
        } else {
            HoldCounter holdInfo = threadHoldTrace.get();
            return holdInfo != null ? holdInfo.holdCount : 0;
        }
    }

    //****************************************************************************************************************//
    //                                  4:inner interface/class(2)                                                    //                                                                                  //
    //****************************************************************************************************************//
    private static class HoldCounter {
        private int holdCount = 0;
    }

    //sharable hold ThreadLocal
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
            this.awaitWithSupport(ThreadParkSupport.create(0, false), true);
        }

        public void awaitUninterruptibly() {
            try {
                this.awaitWithSupport(ThreadParkSupport.create(0, false), false);
            } catch (InterruptedException e) {
                //in fact,InterruptedException never throws here
            }
        }

        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            ThreadParkSupport support = ThreadParkSupport.create(nanosTimeout, false);
            this.awaitWithSupport(support, true);
            return support.getParkTime();
        }

        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            ThreadParkSupport support = ThreadParkSupport.create(unit.toNanos(time), false);
            this.awaitWithSupport(support, true);
            return support.isTimeout();
        }

        public boolean awaitUntil(Date deadline) throws InterruptedException {
            ThreadParkSupport support = ThreadParkSupport.create(deadline.getTime(), true);
            this.awaitWithSupport(support, true);
            return support.isTimeout();
        }

        //do wait
        private void awaitWithSupport(ThreadParkSupport support, boolean throwsIE) throws InterruptedException {
            //1:release lock(state==0)
            this.access.release();//(inside check:thread == hold thread,if not equals then throws IllegalMonitorStateException)

            //2:offer to condition queue and wait for signal from other thread
            ThreadNode conditionNode = super.createNode();
            //we dont't care timeout from this,but interrupted then throws InterruptedException
            super.doWait(support, throwsIE, conditionNode);

            //3:add to syn  wait queue to lock(notify from other)
            try {
                ThreadParkSupport acquireSupport = ThreadParkSupport.create(0, false);
                access.acquireByAction(access.exclusiveAcquireAction, acquireSupport, true, conditionNode);//if failed,not notify wait node in syn queue(@todo.....)
            } catch (InterruptedException e) {
                super.wakeupOne();//wake up a condition wait node(why? because the node has got a condition signal)
            }
        }

        public void signal() {
            if (!access.isExclusiveHeldByCurrentThread()) throw new IllegalMonitorStateException();
            super.wakeupOne();//node wait(step2) in awaitWithSupport
        }

        public void signalAll() {
            if (!access.isExclusiveHeldByCurrentThread()) throw new IllegalMonitorStateException();
            super.wakeupAll();//node wait(step2) in awaitWithSupport
        }
    }
}
