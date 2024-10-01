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

import org.stone.beetp.TaskPoolThreadFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * Task execution worker
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskExecutionWorker extends PoolBaseWorker {
    private final ConcurrentLinkedQueue<PoolTaskHandle<?>> taskBucket;
    private final ConcurrentLinkedQueue<PoolTaskHandle<?>>[] taskBuckets;

    private volatile long completedCount;
    private volatile PoolTaskHandle<?> processingHandle;

    public TaskExecutionWorker(TaskPoolThreadFactory threadFactory,
                               long keepAliveTimeNanos, boolean useTimePark, int defaultSpins,
                               ConcurrentLinkedQueue<PoolTaskHandle<?>> taskBucket,
                               ConcurrentLinkedQueue<PoolTaskHandle<?>>[] taskBuckets) {

        super(threadFactory, keepAliveTimeNanos, useTimePark, defaultSpins);
        this.taskBucket = taskBucket;
        this.taskBuckets = taskBuckets;
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
    public ConcurrentLinkedQueue<PoolTaskHandle<?>> getTaskBucket() {
        return taskBucket;
    }

    /**
     * poll tasks from worker
     *
     * @return a list of polled tasks
     */
    public List<PoolTaskHandle<?>> getUnCompletedTasks() {
        List<PoolTaskHandle<?>> taskList = new LinkedList<>();
        do {
            PoolTaskHandle<?> handle = taskBucket.poll();
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
            //1: poll a task from private queue
            PoolTaskHandle<?> handle = taskBucket.poll();
            //2: steal a task from other worker when poll a null task
            if (handle == null) {
                for (ConcurrentLinkedQueue<PoolTaskHandle<?>> bucket : taskBuckets) {
                    handle = bucket.poll();
                    if (handle != null) break;
                }
            }

            //3: process the polled task
            if (handle != null) {
                this.processingHandle = handle;
                if (handle.setExecutionWorker(this)) {
                    Thread.interrupted();//clear interrupted flag
                    handle.executeTask(this);
                }
                this.processingHandle = null;
                spinSize = defaultSpins;
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
