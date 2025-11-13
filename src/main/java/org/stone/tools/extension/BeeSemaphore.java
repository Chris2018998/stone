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
 * {@link #BeeSemaphore} is a customization semaphore implementation for stone project to improve pool performance.
 * <p>
 * Note: It is a private tool,Forbidden to copy its logic or apply it in other projects.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeSemaphore implements BeeInterruptable {
    // VarHandle mechanics
    private static final VarHandle permitStateHandle;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            permitStateHandle = l.findVarHandle(BeeSemaphorePermit.class, "state", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final boolean fair;
    //Fixed length of Array
    private final BeeSemaphorePermit[] permits;
    //Customization wait queue
    private final BeeTransferQueue waitQueue;

    /**
     * Construct a semaphore with permit size.
     *
     * @param size to be created fixed length array
     */
    public BeeSemaphore(int size) {
        this(size,false);
    }

    /**
     * Construct a semaphore with permit size and specified mode
     *
     * @param size to be created fixed length array
     * @param fair is true that fair mode
     */
    public BeeSemaphore(int size,boolean fair) {
        this.fair = fair;
        this.waitQueue = new BeeTransferQueue();
        this.permits = new BeeSemaphorePermit[size];
        for (int i = 0; i < size; i++) {
            permits[i] = new BeeSemaphorePermit();
        }
    }

    /**
     * Attempt to acquire a permit.
     *
     * @param deadlineNanos is a deadline time value
     * @return acquired permit or null when timeout
     * @throws InterruptedException when interruption occurred during waiting for a released permit
     */
    public BeeSemaphorePermit tryAcquire(long deadlineNanos, BeeTransferQueueNode node) throws InterruptedException {
        if(!fair || waitQueue.existWaiters()) {
            //1: search an idle permit from permit array
            for (BeeSemaphorePermit permit : permits) {
                if (permitStateHandle.compareAndSet(permit, 0, 1)) {
                    return permit;
                }
            }
        }

        //2: Offer node to wait queue
        node.item = null;//set to null to accept a transferred value
        waitQueue.offer(node);
        Thread thread = node.thread;

        //3:self-spin
        do {
            //3.1: read value which maybe filled by a releaser
            Object value = node.item;//(value maybe an exception)
            if (value instanceof BeeSemaphorePermit permit) {
                if (permitStateHandle.compareAndSet(permit, 0, 1)) {
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

    /**
     * Release a permit
     *
     * @param permit to recycled
     */
    public void release(BeeSemaphorePermit permit) {
        permit.state = 0;//set to idle state
        waitQueue.tryTransfer(permit);
    }

    //****************************************************************************************************************//
    //                                        Queue operation of waiting threads                                      //
    //****************************************************************************************************************//
    public List<Thread> getQueuedThreads() {
        return waitQueue.getQueuedThreads();
    }

    public List<Thread> interruptQueuedWaitThreads() {
        return waitQueue.interruptQueuedWaitThreads();
    }
}
