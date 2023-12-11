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

import org.stone.tools.unsafe.UnsafeAdaptorSunMiscImpl;
import sun.misc.Unsafe;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.execution.TaskPoolConstants.*;

/**
 * Task work thread
 *
 * @author Chris Liao
 * @version 1.0
 */
final class TaskWorkThread extends Thread {
    //unsafe to update state field
    private static final Unsafe U;
    private static final long stateOffset;

    static {
        try {
            U = UnsafeAdaptorSunMiscImpl.U;
            stateOffset = U.objectFieldOffset(TaskWorkThread.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    final Queue<BaseHandle> workQueue;
    private final TaskExecutionPool pool;//owner

    volatile long completedCount;//task completed count by this current worker
    volatile BaseHandle curTaskHandle;//task handle in processing
    private volatile Object state;//state definition,@see{@link TaskPoolConstants}

    TaskWorkThread(Object state, TaskExecutionPool pool, int size) {
        this.pool = pool;
        this.state = state;
        this.workQueue = new ConcurrentLinkedQueue();
    }

    //***************************************************************************************************************//
    //                                      1: state(2)                                                              //
    //**************************************************e************************************************************//
    void setState(Object update) {
        this.state = update;
    }

    boolean compareAndSetState(Object expect, Object update) {
        return expect == state && U.compareAndSwapObject(this, stateOffset, expect, update);
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
        final Queue<BaseHandle> queue = pool.getTaskQueue();
        final boolean useTimePark = pool.isIdleTimeoutValid();
        final long idleTimeoutNanos = pool.getIdleTimeoutNanos();

        do {
            //1: read state of worker,if value equals terminated state,then exit
            Object state = this.state;
            if (state == WORKER_TERMINATED) break;

            //2: get a task(from state,individual queue,common queue)
            BaseHandle handle = null;
            if (state instanceof BaseHandle) {
                handle = (BaseHandle) state;
                this.state = WORKER_WORKING;
            } else {
                handle = workQueue.poll();//individual queue
                if (handle == null) handle = queue.poll();//poll from common queue
                if (handle == null) {//steal a task from other workers
                    for (TaskWorkThread worker : pool.getWorkerArray()) {
                        if (worker == this) continue;
                        handle = worker.workQueue.poll();
                        if (handle != null) break;
                    }
                }
            }

            //3: execute task if not be null
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
                this.state = WORKER_IDLE;
                if (useTimePark) {
                    LockSupport.parkNanos(idleTimeoutNanos);
                    if (compareAndSetState(WORKER_IDLE, WORKER_TERMINATED)) break;
                } else {
                    LockSupport.park();
                }
                //Thread.interrupted();//clear interrupted flag and repeat to get dynamic parameters from pool
            }
        } while (true);

        //remove worker from pool
        pool.removeTaskWorker(this);
    }
}
