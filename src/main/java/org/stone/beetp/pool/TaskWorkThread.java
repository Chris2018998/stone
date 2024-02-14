/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.pool;

import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.TaskPoolConstants.*;

/**
 * Pooled thread to execute submission tasks
 *
 * @author Chris Liao
 * @version 1.0
 */
final class TaskWorkThread extends Thread {
    private static final AtomicReferenceFieldUpdater<TaskWorkThread, Object> StateUpd = ReferenceFieldUpdaterImpl.newUpdater(TaskWorkThread.class, Object.class, "state");
    final Queue<BaseHandle> workQueue;
    private final TaskExecutionPool pool;//owner
    volatile long completedCount;//task completed count by this current worker
    volatile BaseHandle curTaskHandle;//task handle in processing
    private volatile Object state;//state definition,@see{@link TaskPoolConstants}

    TaskWorkThread(Object state, TaskExecutionPool pool) {
        this.pool = pool;
        this.state = state;
        this.workQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                      1: state(2)                                                              //
    //**************************************************e************************************************************//
    void setState(Object update) {
        this.state = update;
    }

    boolean compareAndSetState(Object expect, Object update) {
        return expect == state && StateUpd.compareAndSet(this, expect, update);
    }

    //***************************************************************************************************************//
    //                                      2: thread interruptBlocking(1)                                           //
    //**************************************************e************************************************************//
    void interruptBlocking(BaseHandle taskHandle) {
        if (taskHandle == this.curTaskHandle) this.interrupt();
    }

    //***************************************************************************************************************//
    //                                      3: thread work methods(2)                                                 //
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
            BaseHandle handle;
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
                    final long deadline = System.nanoTime() + idleTimeoutNanos;
                    LockSupport.parkNanos(idleTimeoutNanos);//may bei park failed ?
                    if (deadline - System.nanoTime() <= 0L && compareAndSetState(WORKER_IDLE, WORKER_TERMINATED))
                        break;
                } else {
                    LockSupport.park();
                }
                //Thread.interrupted();//clear interrupted flag and repeat to get dynamic parameters from pool
            }
        } while (true);

        //remove worker from pool
        pool.removeTaskWorker(this);
    }


//    public void run() {
//        final Queue<BaseHandle> queue = pool.getTaskQueue();
//        final boolean useTimePark = pool.isIdleTimeoutValid();
//        final long idleTimeoutNanos = pool.getIdleTimeoutNanos();
//
//        do {
//            //1: read state of worker,if value equals terminated state,then exit
//            Object state = this.state;
//            if (state == WORKER_TERMINATED) break;
//
//            //2: convert state object to handle and process it
//            if (state instanceof BaseHandle)e
//                this.processTask((BaseHandle) state);
//            //3: poll from individual queue
//            BaseHandle taskHandle;
//            while ((taskHandle = workQueue.poll()) != null)
//                this.processTask(taskHandle);
//            //4: poll from common queue
//            while ((taskHandle = queue.poll()) != null)
//                this.processTask(taskHandle);
//            //5: steal task from other workers queue
//            for (TaskWorkThread worker : pool.getWorkerArray()) {
//                if (worker == this) continue;
//                Queue<BaseHandle> stealQ = worker.workQueue;
//                while ((taskHandle = stealQ.poll()) != null)
//                    this.processTask(taskHandle);
//            }
//
//            //6: set worker sate to idle and park thread
//            this.state = WORKER_IDLE;
//            if (useTimePark) {
//                final long deadline = System.nanoTime() + idleTimeoutNanos;
//                LockSupport.parkNanos(idleTimeoutNanos);//maybe park failed
//                if (deadline - System.nanoTime() <= 0L && compareAndSetState(WORKER_IDLE, WORKER_TERMINATED))
//                    break;
//            } else {
//                LockSupport.park();
//            }
//        } while (true);
//
//        //remove worker from pool
//        pool.removeTaskWorker(this);
//    }
//
//    private void processTask(BaseHandle taskHandle) {
//        if (taskHandle.setAsRunning(this)) {//maybe cancellation concurrent,so cas state
//            try {
//                taskHandle.beforeExecute();
//                taskHandle.executeTask(this);
//            } finally {
//                this.curTaskHandle = null;
//                taskHandle.afterExecute(this);
//            }
//        }
//    }
}
