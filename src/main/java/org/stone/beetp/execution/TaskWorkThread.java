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

    private final TaskExecutionPool pool;
    private final ConcurrentLinkedQueue<JoinTaskHandle> joinSubTaskQueue;
    private final ConcurrentLinkedQueue<TreeTaskHandle> treeSubTaskQueue;
    volatile Object state;//state of work thread
    volatile long completedCount;//completed count of tasks by thread
    volatile BaseHandle curTaskHandle;//task handle in processing

    TaskWorkThread(Object state, TaskExecutionPool pool, boolean workInDaemon, String poolName) {
        this.pool = pool;
        this.state = state;
        this.setDaemon(workInDaemon);
        this.setName(poolName + "-task worker");

        this.joinSubTaskQueue = new ConcurrentLinkedQueue<>();
        this.treeSubTaskQueue = new ConcurrentLinkedQueue<>();
    }

    void setState(Object update) {
        this.state = update;
    }

    boolean compareAndSetState(Object expect, Object update) {
        return expect == state && workerStateUpd.compareAndSet(this, expect, update);
    }

    void interrupt(BaseHandle taskHandle) {
        if (taskHandle == this.curTaskHandle)
            this.interrupt();
    }


    void pushSubTaskHandle(JoinTaskHandle handle) {
        joinSubTaskQueue.offer(handle);
    }

    public void run() {
        final long idleTimeoutNanos = pool.getIdleTimeoutNanos();
        final ConcurrentLinkedQueue<BaseHandle> executionQueue = pool.getTaskExecutionQueue();

        do {
            //1: read state from work
            //if (poolState >= POOL_TERMINATING) break;
            Object state = this.state;//exits repeat read same
            if (state == WORKER_TERMINATED) break;

            //2: get task from state or poll from queue
            BaseHandle handle = null;
            if (state instanceof BaseHandle) {//handle must be not null
                handle = (BaseHandle) state;
                this.state = WORKER_WORKING;
            }
            if (handle == null) handle = joinSubTaskQueue.poll();
            if (handle == null) handle = treeSubTaskQueue.poll();
            if (handle == null) handle = executionQueue.poll();
            if (handle == null) handle = stealTaskFromOther();

            //3: execute task
            if (handle != null) {
                if (handle.setAsRunning(this)) {//maybe cancellation concurrent,so cas state
                    try {
                        handle.beforeExecute();
                        handle.executeTask(this);
                    } finally {
                        handle.afterExecute(this);
                        this.curTaskHandle = null;
                    }
                }
            } else {//4: park work thread
                this.state = WORKER_IDLE;//set to be idle from WORKER_WORKING
                if (idleTimeoutNanos > 0L) {
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

    private BaseHandle stealTaskFromOther() {
        TaskWorkThread[] threads = pool.workerArray;
        for (TaskWorkThread thread : threads) {
            if (thread!=null && thread != this) {
                BaseHandle handle = thread.joinSubTaskQueue.poll();
                if (handle != null) return handle;
                handle = thread.treeSubTaskQueue.poll();
                if (handle != null) return handle;
            }
        }
        return null;
    }
}
