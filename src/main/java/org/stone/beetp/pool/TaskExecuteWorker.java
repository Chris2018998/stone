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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * Pool task execution worker
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskExecuteWorker extends TaskBucketWorker {
    //stores some tasks pushed from ownerPool by call its {@link #put()}methods
    private final ConcurrentLinkedQueue<PoolTaskHandle<?>> taskQueue;

    //a task handle in being processed by this worker
    private PoolTaskHandle<?> taskHandle;

    public TaskExecuteWorker(PoolTaskCenter pool) {
        super(pool);
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods(2)                                               //
    //***************************************************************************************************************//

    /**
     * Pool push a task to worker by call its method
     *
     * @param taskHandle is a handle passed from pool
     */
    public void put(PoolTaskHandle<?> taskHandle) {
        //1: offer task handle to queue
        taskQueue.offer(taskHandle);
        //2: set this worker to task handle as owner bucket
        taskHandle.setTaskBucket(this);

        //3: notify internal thread to run this task
        int curState = state;
        if (curState != WORKER_RUNNING && StateUpd.compareAndSet(this, curState, WORKER_RUNNING)) {
            if (WORKER_WAITING == curState) {//unkpark thread
                LockSupport.unpark(workThread);
            } else if (WORKER_DEAD == curState) {//create or re-create a thread to run task
                this.workThread = new Thread(this);
                this.workThread.start();
            }
        }
    }

    /**
     * cancel a given task from this worker
     *
     * @param taskHandle            to be cancelled
     * @param mayInterruptIfRunning is true that interrupt blocking in execupting if exists
     * @return true cancel succesful
     */
    public boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning) {
        return true;//@todo to be implemented
    }

    //***************************************************************************************************************//
    //                                             2: task process method(core)                                      //
    //***************************************************************************************************************//
    public void run() {
        final boolean useTimePark = pool.isIdleTimeoutValid();
        final long idleTimeoutNanos = pool.getIdleTimeoutNanos();
        final TaskExecuteWorker[] allWorkers = pool.getExecuteWorkers();

        do {
            //1: check worker state,if dead then exit from loop
            if (state == WORKER_DEAD) {
                this.workThread = null;
                break;
            }

            //2: attempt to poll a task from queue
            PoolTaskHandle<?> handle = taskQueue.poll();

            //3: attempt to poll a task from other worker's queue if poll out a null task
            if (handle == null) {//steal a task from other workers
                for (TaskExecuteWorker worker : allWorkers) {
                    if (worker == this) continue;
                    handle = worker.taskQueue.poll();
                    if (handle != null) break;
                }
            }

            //4: clear interrupted flag of this worker thread if it exists
            if (workThread.isInterrupted() && Thread.interrupted()) {
                //no code here
            }

            //5: process the pulled task
            if (handle != null) {
                this.taskHandle = handle;//put running task to local field
                handle.executeTask(this);
                this.taskHandle = null;//reset local field to null after completion
            } else if (StateUpd.compareAndSet(this, WORKER_RUNNING, WORKER_WAITING)) {//cas stete to waiting
                boolean parkTimeout = false;
                if (useTimePark) {
                    long parkStartTime = System.nanoTime();
                    LockSupport.parkNanos(idleTimeoutNanos);
                    //check timeout with elapsed time on park
                    parkTimeout = System.nanoTime() - parkStartTime >= idleTimeoutNanos;
                } else {
                    LockSupport.park();
                }

                //6: worker state check after park
                if (state == WORKER_WAITING) {//park timeout,interrupted,park fail
                    if (parkTimeout) {
                        StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_DEAD);
                    } else {
                        StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_RUNNING);
                    }
                }
            }
        } while (true);
    }
}
