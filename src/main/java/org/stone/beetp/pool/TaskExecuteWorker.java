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
    private final ConcurrentLinkedQueue<PoolTaskHandle<?>> taskQueue;
    private volatile PoolTaskHandle<?> processingHandle;

    public TaskExecuteWorker(PoolTaskCenter pool) {
        super(pool);
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods(2)                                               //
    //***************************************************************************************************************//

    /**
     * get task handle in processing by this worker
     *
     * @return handle in processing,if not,then return null
     */
    public PoolTaskHandle getProcessingHandle() {
        return processingHandle;
    }

    /**
     * Pool push a task to worker by call this method
     *
     * @param taskHandle is a handle passed from pool
     */
    public void put(PoolTaskHandle<?> taskHandle) {
        //1: offer task handle to queue
        taskQueue.offer(taskHandle);
        //2: set this worker to task handle as owner bucket
        taskHandle.setTaskBucket(this);
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
        int spinSize = pool.getWorkerSpins();
        final long keepAliveTimeNanos = pool.getKeepAliveTimeNanos();
        final TaskExecuteWorker[] allWorkers = pool.getExecuteWorkers();
        final boolean useTimePark = keepAliveTimeNanos > 0L;

        do {
            //1: poll a task from queue
            PoolTaskHandle<?> handle = taskQueue.poll();
            if (handle == null) {//steal a task from other worker
                for (TaskExecuteWorker worker : allWorkers) {
                    handle = worker.taskQueue.poll();
                    if (handle != null) break;
                }
            }

            //2: proccess the polled task
            if (handle != null) {
                this.processingHandle = handle;
                handle.executeTask(this);//@todo clear interrupted flag inside method
                this.processingHandle = null;
            } else if (spinSize > 0) {
                spinSize--;
            } else {
                //3: park work thread
                spinSize = pool.getWorkerSpins();
                if (StateUpd.compareAndSet(this, WORKER_RUNNING, WORKER_WAITING)) {
                    if (useTimePark) {
                        final long parkStartTime = System.nanoTime();
                        LockSupport.parkNanos(keepAliveTimeNanos);
                        if (System.nanoTime() - parkStartTime >= keepAliveTimeNanos && StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_INACTIVE))
                            break;
                    } else {
                        LockSupport.park();
                    }

                    //4: clear interrupted flag if it exist
                    if (workThread.isInterrupted() && Thread.interrupted()) {
                        //no code here
                    }

                    //5: set running state
                    if (state == WORKER_WAITING) StateUpd.compareAndSet(this, WORKER_WAITING, WORKER_RUNNING);
                }
            }
        } while (state != WORKER_INACTIVE);
    }
}
