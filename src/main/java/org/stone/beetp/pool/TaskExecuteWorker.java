/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * Pool task execution worker
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskExecuteWorker implements PoolTaskBucket, Runnable {
    private static final int STATE_DEAD = 0;
    private static final int STATE_WAITING = 1;
    private static final int STATE_WORKING = 2;
    private static final AtomicIntegerFieldUpdater<TaskExecuteWorker> StateUpd = IntegerFieldUpdaterImpl.newUpdater(TaskExecuteWorker.class, "state");

    //owner pool of this worker
    private final PoolTaskCenter ownerPool;
    //stores some tasks pushed from ownerPool
    private final ConcurrentLinkedQueue<PoolTaskHandle<?>> taskQueue;

    /**
     * state map lines
     * line1: STATE_WORKING ---> STATE_WAITING ---> STATE_WORKING (main line)
     * line2: STATE_WORKING ---> STATE_WAITING ---> STATE_DEAD
     * line3: STATE_WORKING ---> STATE_DEAD
     */
    private volatile int state;
    //a task handle in being processed by this worker
    private PoolTaskHandle<?> taskHandle;
    //work thread of this worker
    private Thread workThread;

    public TaskExecuteWorker(PoolTaskCenter ownerPool) {
        this.ownerPool = ownerPool;
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods                                                  //
    //***************************************************************************************************************//

    /**
     * Pool call this method to push a task to worker.
     *
     * @param taskHandle is a handle passed from pool
     */
    public void put(PoolTaskHandle<?> taskHandle) {
        taskQueue.offer(taskHandle);
        taskHandle.setTaskBucket(this);//set bucket to handle if not exists

        //wake up work thread to execute this task
        int curState = state;
        if (curState != STATE_WORKING && StateUpd.compareAndSet(this, curState, STATE_WORKING)) {
            if (STATE_WAITING == curState) {
                LockSupport.unpark(workThread);
            } else if (STATE_DEAD == curState) {//reactivate work thread
                this.workThread = new Thread(this);
                this.workThread.start();
            }
        }
    }

    /**
     * cancel a given task from this worker
     *
     * @param taskHandle            is handle of a task
     * @param mayInterruptIfRunning is true that worker thread is interrupted if task blocking in process
     * @return true if cancel success;otherwise return false
     */
    public boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning) {
        return true;//@todo to be implemented
    }

    //***************************************************************************************************************//
    //                                             2: core method to process tasks                                   //
    //***************************************************************************************************************//
    public void run() {
        final boolean useTimePark = ownerPool.isIdleTimeoutValid();
        final long idleTimeoutNanos = ownerPool.getIdleTimeoutNanos();
        final TaskExecuteWorker[] allWorkers = ownerPool.getExecuteWorkers();

        do {
            //1:check worker state,if dead then exit loop
            if (state == STATE_DEAD) break;
            //clear interrupted flag,if it exists
            if (workThread.isInterrupted() && Thread.interrupted()) {
                //no code here,just clear flag of interruption
            }

            //2: poll a task from queue of this worker
            PoolTaskHandle<?> handle = taskQueue.poll();

            //3: poll task from other workers
            if (handle == null) {//steal a task from other workers
                for (TaskExecuteWorker worker : allWorkers) {
                    if (worker == this) continue;
                    handle = worker.taskQueue.poll();
                    if (handle != null) break;
                }
            }

            //4: process this task or park working thread if no task
            if (handle != null) {
                this.taskHandle = handle;//record the pull task

                //attempt to execute this task(under cas success)
                handle.executeTask(this);

                this.taskHandle = null;//reset to null
            } else if (StateUpd.compareAndSet(this, STATE_WORKING, STATE_WAITING)) {//park work thread if cas successful
                boolean timeout = false;
                if (useTimePark) {
                    long parkStartTime = System.nanoTime();
                    LockSupport.parkNanos(idleTimeoutNanos);
                    timeout = System.nanoTime() - parkStartTime >= idleTimeoutNanos;
                } else {
                    LockSupport.park();
                }

                //state check after park
                if (state == STATE_WAITING) {//two possibility: park fail or interrupted
                    if (timeout) {
                        if (StateUpd.compareAndSet(this, STATE_WAITING, STATE_DEAD))
                            break;//break out while(work thread become terminated)
                    } else {
                        StateUpd.compareAndSet(this, STATE_WAITING, STATE_WORKING);
                    }
                }
            }
        } while (true);
    }
}
