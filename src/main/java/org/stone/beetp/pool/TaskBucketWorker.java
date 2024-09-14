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

/**
 * Base pool worker,which works as a bucket to store some tasks and process them
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class TaskBucketWorker extends ReactivateWorker {
    protected static final List<PoolTaskHandle<?>> emptyList = new LinkedList<>();
    protected volatile long completedCount;

    public TaskBucketWorker(PoolTaskCenter pool, long keepAliveTimeNanos, boolean useTimePark, int defaultSpins) {
        super(pool, keepAliveTimeNanos, useTimePark, defaultSpins);
    }

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
        return ++completedCount;
    }

    /**
     * get a list of uncompleted task
     *
     * @return a list of uncompleted tasks
     */
    abstract List<PoolTaskHandle<?>> getUnCompletedTasks();

    /**
     * Pool push a task to worker by call this method
     *
     * @param taskHandle is a handle passed from pool
     */
    abstract void put(PoolTaskHandle<?> taskHandle);

    /**
     * cancel a given task from this worker
     *
     * @param taskHandle            to be cancelled
     * @param mayInterruptIfRunning is true that interrupt blocking in executing if exists
     * @return true cancel successful
     */
    abstract boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning);
}
