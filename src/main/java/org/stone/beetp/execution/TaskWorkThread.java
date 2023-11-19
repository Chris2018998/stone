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

import org.stone.beetp.Task;
import org.stone.beetp.TreeTask;

import java.util.concurrent.ConcurrentLinkedDeque;
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
    protected ConcurrentLinkedDeque<Task> joinSubTaskQueue;
    protected ConcurrentLinkedDeque<TreeTask> treeSubTaskQueue;

    volatile Object state;
    volatile BaseHandle curTaskHandle;//in processing task handle

    TaskWorkThread(Object state, TaskExecutionPool pool, boolean workInDaemon, String poolName) {
        this.pool = pool;
        this.state = state;
        this.setDaemon(workInDaemon);
        this.setName(poolName + "-task worker");
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

    public void run() {
        final long idleTimeoutNanos = pool.getIdleTimeoutNanos();
        final ConcurrentLinkedQueue<BaseHandle> executionQueue = pool.getTaskExecutionQueue();

        do {
            //1: read state from work
            //if (poolState >= POOL_TERMINATING) break;
            Object state = this.state;//exits repeat read same
            if (state == WORKER_TERMINATED) break;

            //2: get task from state or poll from queue
            BaseHandle handle;
            if (state instanceof BaseHandle) {//handle must be not null
                handle = (BaseHandle) state;
                this.state = WORKER_WORKING;
            } else {
                handle = executionQueue.poll();//may be poll null from queue
            }

            //3: execute task
            if (handle != null) {
                if (handle.setAsRunning(this)) {//maybe cancellation concurrent,so cas state
                    try {
                        handle.beforeExecute();
                        handle.executeTask();
                    } finally {
                        handle.afterExecute();
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
}
