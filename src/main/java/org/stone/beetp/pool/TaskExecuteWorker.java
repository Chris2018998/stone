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

import java.util.LinkedList;
import java.util.List;
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
    private final TaskExecuteWorker[] executeWorkers;
    private final ConcurrentLinkedQueue<PoolTaskHandle<?>> taskQueue;
    private volatile long completedCount;
    private PoolTaskHandle<?> processingHandle;

    public TaskExecuteWorker(long keepAliveTimeNanos, boolean useTimePark, int defaultSpins, TaskExecuteWorker[] executeWorkers) {
        super(keepAliveTimeNanos, useTimePark, defaultSpins);
        this.executeWorkers = executeWorkers;
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods(2)                                               //
    //***************************************************************************************************************//

    /**
     * query this completed count of tasks
     *
     * @return completed count
     */
    public long getCompletedCount() {
        return completedCount;
    }

    /**
     * increment completed count of tasks
     *
     * @return completed count
     */
    public long incrementCompletedCount() {
        long count = completedCount;
        if (++count < 0) {
            completedCount = 1;//reset to 1 when exceeded
        } else {
            completedCount = count;
        }
        return completedCount;
    }

    /**
     * get task handle in processing by this worker
     *
     * @return handle in processing,if not,then return null
     */
    public PoolTaskHandle<?> getProcessingHandle() {
        return processingHandle;
    }

    /**
     * get task queue
     *
     * @return task queue
     */
    public ConcurrentLinkedQueue<PoolTaskHandle<?>> getQueue() {
        return taskQueue;
    }

    /**
     * Pool push a task to worker by call this method
     *
     * @param taskHandle is a handle passed from pool
     */
    public void put(PoolTaskHandle<?> taskHandle) {
        taskHandle.setTaskBucket(this);
        taskQueue.offer(taskHandle);
    }

    /**
     * poll tasks from worker
     *
     * @return a list of polled tasks
     */
    public List<PoolTaskHandle<?>> getUnCompletedTasks() {
        List<PoolTaskHandle<?>> taskList = new LinkedList<>();
        do {
            PoolTaskHandle<?> handle = taskQueue.poll();
            if (handle == null) break;
            taskList.add(handle);
        } while (true);
        return taskList;
    }

    /**
     * cancel a given task from this worker
     *
     * @param taskHandle            to be cancelled
     * @param mayInterruptIfRunning is true that interrupt blocking in executing if exists
     * @return true cancel successful
     */
    public boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning) {
        return true;//@todo to be implemented
    }

    //***************************************************************************************************************//
    //                                             2: task process method(core)                                      //
    //***************************************************************************************************************//
    public void run() {
        int spinSize = defaultSpins;

        do {
            //1: poll a task from queue
            PoolTaskHandle<?> handle = taskQueue.poll();
            if (handle == null) {//steal a task from other worker
                for (TaskExecuteWorker worker : executeWorkers) {
                    handle = worker.taskQueue.poll();
                    if (handle != null) break;
                }
            }

            //2: process the polled task
            if (handle != null) {
                this.processingHandle = handle;
                if (handle.setRunWorker(this)) {
                    Thread.interrupted();//clear interrupted flag
                    handle.executeTask(this);
                    spinSize = defaultSpins;
                }
                this.processingHandle = null;
            } else if (spinSize > 0) {
                spinSize--;
            } else {
                //3: park work thread
                if (StateUpd.compareAndSet(this, WORKER_RUNNING, WORKER_WAITING)) {
                    int resetState = WORKER_RUNNING;
                    Thread.interrupted();//clear interrupted flag
                    if (useTimePark) {
                        final long parkStartTime = System.nanoTime();
                        LockSupport.parkNanos(keepAliveTimeNanos);
                        if (System.nanoTime() - parkStartTime >= keepAliveTimeNanos) resetState = WORKER_PASSIVATED;
                    } else {
                        LockSupport.park();
                    }

                    //reset state
                    if (state == WORKER_WAITING && StateUpd.compareAndSet(this, WORKER_WAITING, resetState) && resetState == WORKER_PASSIVATED)
                        break;
                }
                spinSize = defaultSpins;//reset spin size to default
            }
        } while (state != WORKER_PASSIVATED);
    }
}
