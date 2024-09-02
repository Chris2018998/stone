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

import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.stone.beetp.pool.PoolConstants.WORKER_DEAD;

/**
 * Base pool worker,which works as a bucket to store some tasks and process them
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class TaskBucketWorker implements Runnable {
    protected static final AtomicIntegerFieldUpdater<TaskBucketWorker> StateUpd = IntegerFieldUpdaterImpl.newUpdater(TaskBucketWorker.class, "state");
    protected static final List<PoolTaskHandle<?>> emptyList = new LinkedList<>();
    protected final PoolTaskCenter pool;

    protected int state;
    protected Thread workThread;

    //constructor with pool
    public TaskBucketWorker(PoolTaskCenter pool) {
        this.state = WORKER_DEAD;
        this.pool = pool;
    }

    /**
     * terminate worker and make it stop working
     *
     * @return an uncompleted list of tasks after terminated
     */
    abstract List<PoolTaskHandle<?>> terminate();

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
