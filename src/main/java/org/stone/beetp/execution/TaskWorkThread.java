/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.execution;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.execution.TaskPoolConstants.*;

/**
 * Task work thread
 *
 * @author Chris Liao
 * @version 1.0
 */
final class TaskWorkThread extends Thread {
    private static final AtomicReferenceFieldUpdater<TaskWorkThread, Object> workerStateUpd = AtomicReferenceFieldUpdater.newUpdater(TaskWorkThread.class, Object.class, "state");
    final ConcurrentLinkedQueue<BaseHandle> workQueue;//support stealing
    private final TaskExecutionPool pool;

    volatile Object state;//state of work thread
    volatile long completedCount;//completed count of tasks by thread
    volatile BaseHandle curTaskHandle;//task handle in processing

    TaskWorkThread(Object state, TaskExecutionPool pool, boolean workInDaemon, String poolName) {
        this.pool = pool;
        this.state = state;
        this.setDaemon(workInDaemon);
        this.setName(poolName + "-task worker");

        this.workQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                      1: state(2)                                                              //
    //**************************************************e************************************************************//
    void setState(Object update) {
        this.state = update;
    }

    boolean compareAndSetState(Object expect, Object update) {
        return expect == state && workerStateUpd.compareAndSet(this, expect, update);
    }

    //***************************************************************************************************************//
    //                                      2: thread interruptBlocking(1)                                           //
    //**************************************************e************************************************************//
    void interruptBlocking(BaseHandle taskHandle) {
        if (taskHandle == this.curTaskHandle) this.interrupt();
    }

    //***************************************************************************************************************//
    //                                      3: thead work methods(2)                                                 //
    //**************************************************e************************************************************//
    public void run() {
        final long idleTimeoutNanos = pool.getIdleTimeoutNanos();
        final boolean useTimePark = idleTimeoutNanos > 0L;
        final ConcurrentLinkedQueue<BaseHandle> executionQueue = pool.getTaskExecutionQueue();

        do {
            //1: read state from work
            Object state = this.state;//exits repeat read same
            if (state == WORKER_TERMINATED) break;

            //2: get task from state or poll from queue
            BaseHandle handle;
            if (state instanceof BaseHandle) {//handle must be not null
                handle = (BaseHandle) state;
                this.state = WORKER_WORKING;
            } else {
                handle = workQueue.poll();
                if (handle == null) handle = executionQueue.poll();
                if (handle == null) {//steal a task from other work thread
                    for (TaskWorkThread thread : pool.getWorkerArray()) {
                        if (thread != null && thread != this) {
                            handle = thread.workQueue.poll();
                            if (handle != null) break;
                        }
                    }
                }
            }

            //3: execute task
            if (handle != null) {
                if (handle.setAsRunning(this)) {//maybe cancellation concurrent,so cas state
                    try {
                        handle.beforeExecute();
                        handle.executeTask(this);
                    } finally {
                        this.curTaskHandle = null;
                        handle.afterExecute(this);
                    }
                }
            } else {//4: park work thread
                this.state = WORKER_IDLE;//set to be idle from WORKER_WORKING
                if (useTimePark) {
                    LockSupport.parkNanos(idleTimeoutNanos);
                    if (compareAndSetState(WORKER_IDLE, WORKER_TERMINATED)) break;//set to be terminated if timeout
                } else {
                    LockSupport.park();
                }
            }

            Thread.interrupted();//clean possible interruption state
        } while (true);

        //remove self from worker array
        pool.removeTaskWorker(this);
    }
}
