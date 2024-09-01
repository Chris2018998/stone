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
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.WORKER_DEAD;

/**
 * Base pool worker,which works as a bucket to store some tasks and process them
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class TaskBucketWorker implements Runnable {
    protected static final AtomicIntegerFieldUpdater<TaskBucketWorker> StateUpd = IntegerFieldUpdaterImpl.newUpdater(TaskBucketWorker.class, "state");
    private static final List<PoolTaskHandle<?>> emptyList = new LinkedList<>();
    //owner pool of this worker
    protected final PoolTaskCenter pool;

    /**
     * state map lines
     * line1: WORKER_DEAD ---> WORKER_WORKING ---> WORKER_WAITING (main line)
     * line2: WORKER_WORKING ---> WORKER_WAITING ---> STATE_DEAD
     * line3: WORKER_WORKING ---> WORKER_DEAD
     */
    protected int state;
    //work thread of this worker
    protected Thread workThread;

    //constructor with pool
    public TaskBucketWorker(PoolTaskCenter pool) {
        this.state = WORKER_DEAD;
        this.pool = pool;
    }

    /**
     * terminate worker and make it stop working
     *
     * @return an un-run list of tasks after terminated
     */
    public List<PoolTaskHandle<?>> terminate() {
        int curState = state;
        if (curState == WORKER_DEAD) return emptyList;
        if (StateUpd.compareAndSet(this, curState, WORKER_DEAD)) {
            LockSupport.unpark(workThread);
            return pollAllTasks();
        } else {
            return emptyList;
        }
    }

    /**
     * poll tasks from worker
     *
     * @return a list of polled tasks
     */
    abstract List<PoolTaskHandle<?>> pollAllTasks();

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
     * @param mayInterruptIfRunning is true that interrupt blocking in execupting if exists
     * @return true cancel successful
     */
    abstract boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning);

}
