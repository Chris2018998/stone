/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.extension;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.stone.tools.CommonUtil.SPIN_FOR_TIMEOUT_THRESHOLD;

/**
 * {@link #BeeSemaphore} is a customization semaphore implementation to only support one permit acquisition
 * <p>
 * JDK work mode：wakeup on first node's thread to get permit(one by one,serial mode to reduce concurrent on atomic state)
 * <p>
 * Transfer real permits to waiters with Parallel mode.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeSemaphore implements BeeInterruptable {

    //****************************************************************************************************************//
    //                                        Synchronizer classes                                                    //
    //****************************************************************************************************************//
    //special node value
    static final Object NULL = new Object();
    //synchronizer
    private final UnfairSemaphore synchronizer;

    /**
     * Construct a semaphore with permit size.
     *
     * @param size to be created fixed length array
     */
    public BeeSemaphore(int size) {
        this(size, false);
    }

    /**
     * Construct a semaphore with permit size and specified mode
     *
     * @param size to be created fixed length array
     * @param fair is true that fair mode
     */
    public BeeSemaphore(int size, boolean fair) {
        this.synchronizer = fair ? new FairSemaphore(size) : new UnfairSemaphore(size);
    }

    /**
     * Attempt to acquire a permit.
     *
     * @param deadlineNanos is a deadline time value
     * @return acquired permit or null when timeout
     * @throws InterruptedException when interruption occurred during waiting for a released permit
     */
    public BeeSemaphorePermit tryAcquire(long deadlineNanos, BeeTransferQueueNode node) throws InterruptedException {
        return this.synchronizer.tryAcquire(deadlineNanos, node);
    }

    /**
     * Release a permit
     *
     * @param permit to recycled
     */
    public void release(BeeSemaphorePermit permit) {
        this.synchronizer.release(permit);
    }

    public List<Thread> getQueuedThreads() {
        return synchronizer.getQueuedThreads();
    }

    public List<Thread> interruptQueuedWaitThreads() {
        return synchronizer.interruptQueuedWaitThreads();
    }

    //****************************************************************************************************************//
    //                                        CAS method                                                              //
    //****************************************************************************************************************//
    private boolean casTail(WaiterNode newTail) {
        return true;
    }

    private boolean enqueue(WaiterNode node) {
        return true;
    }

    private boolean denqueue(WaiterNode node) {
        return true;
    }

    //****************************************************************************************************************//
    //                                        Synchronizer classes                                                    //
    //****************************************************************************************************************//
    private static class UnfairSemaphore {
        protected static final VarHandle permitStateHandle;

        static {
            try {
                permitStateHandle = MethodHandles.lookup().findVarHandle(BeeSemaphorePermit.class, "state", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        //Fixed length Array + Hash Index
        protected final BeeSemaphorePermit[] permits;
        //Customization wait queue
        protected final BeeTransferQueue waitQueue;

        UnfairSemaphore(int size) {
            this.waitQueue = new BeeTransferQueue();
            this.permits = new BeeSemaphorePermit[size];
            for (int i = 0; i < size; i++) {
                permits[i] = new BeeSemaphorePermit();
            }
        }

        public List<Thread> getQueuedThreads() {
            return waitQueue.getQueuedThreads();
        }

        public List<Thread> interruptQueuedWaitThreads() {
            return waitQueue.interruptQueuedWaitThreads();
        }

        protected BeeSemaphorePermit search() {
            for (BeeSemaphorePermit permit : permits) {
                if (permit.state == 0 && permitStateHandle.compareAndSet(permit, 0, 1)) {
                    return permit;
                }
            }
            return null;
        }

        protected boolean hold(BeeSemaphorePermit permit) {
            return permit.state == 0 && permitStateHandle.compareAndSet(permit, 0, 1);
        }

        protected void release(BeeSemaphorePermit permit) {
            permit.state = 0;
            waitQueue.tryTransfer(null, permit);
        }

        /**
         * Attempt to acquire a permit.
         *
         * @param deadlineNanos is a deadline time value
         * @return acquired permit or null when timeout
         * @throws InterruptedException when interruption occurred during waiting for a released permit
         */
        public BeeSemaphorePermit tryAcquire(long deadlineNanos, BeeTransferQueueNode node) throws InterruptedException {
            BeeSemaphorePermit permit1 = search();
            if (permit1 != null) return permit1;

            //2: Offer node to wait queue
            //node.item = null;//set to null to accept a transferred value
            Thread thread = Thread.currentThread();
            node = new BeeTransferQueueNode(thread);
            node.item = null;
            waitQueue.offer(node);
            deadlineNanos = System.nanoTime() + deadlineNanos;


            //3:self-spin
            do {
                //3.1: read value which maybe filled by a releaser
                Object value = node.item;//(value maybe an exception)
                if (value instanceof BeeSemaphorePermit permit) {
                    if (hold(permit)) {
                        this.waitQueue.remove(node);
                        return permit;
                    }
                }

                //3.2 block thread via park
                long remainTime = deadlineNanos - System.nanoTime();
                if (remainTime > SPIN_FOR_TIMEOUT_THRESHOLD) {//park node thread
                    if (value != null) node.item = null;//set value to null to be filled by a releaser
                    LockSupport.parkNanos(remainTime);
                    if (thread.isInterrupted() && Thread.interrupted()) {//handle interruption
                        this.waitQueue.remove(node);//remove first.

                        value = node.item;
                        if (value instanceof BeeSemaphorePermit permit) {
                            if (permitStateHandle.compareAndSet(permit, 0, 1)) {
                                release(permit);
                            }
                        }
                        throw new InterruptedException();
                    }
                } else if (remainTime > 0L) {//remain time is too short then use Thread.onSpinWait()
                    if (value != null) node.item = null;//set to null for be filled
                    Thread.onSpinWait();//spin wait
                } else {//timeout
                    this.waitQueue.remove(node);//remove node at first.
                    if (value != null) return null;//if value read out at step3.1,so can't be filled by releaser

                    value = node.item;//read value
                    if (value instanceof BeeSemaphorePermit permit) {//OK,I got a released permit,so cas it
                        if (permitStateHandle.compareAndSet(permit, 0, 1)) {
                            return permit;
                        }
                    }

                    //result for timeout
                    return null;
                }
            } while (true);
        }

    }

    private static class FairSemaphore extends UnfairSemaphore {//Fair Implementation

        FairSemaphore(int size) {
            super(size);
        }

        protected BeeSemaphorePermit search() {
            if (waitQueue.existWaiters()) return null;

            for (BeeSemaphorePermit permit : permits) {
                if (permit.state == 0 && permitStateHandle.compareAndSet(permit, 0, 1)) {
                    return permit;
                }
            }
            return null;
        }

        protected boolean hold(BeeSemaphorePermit permit) {
            return true;
        }

        protected void release(BeeSemaphorePermit permit) {
            if (!waitQueue.tryTransfer(null, permit)) {
                permitStateHandle.setVolatile(permit, 0);//set to idle state
            }
        }
    }

    //Chain Node Class
    private static class WaiterNode {
        //previous node
        public volatile WaiterNode prev;
        //Next node
        public volatile WaiterNode next;

        //Set NULL when node leave from chain
        public volatile Object item = NULL;

        //Set null when node leave from chain
        private Thread thread;

        public WaiterNode(Thread thread) {
            this.thread = thread;
        }
    }
}
