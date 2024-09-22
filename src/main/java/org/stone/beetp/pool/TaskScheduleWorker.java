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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static org.stone.beetp.pool.PoolConstants.*;

/**
 * Pool worker to schedule timed tasks.
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskScheduleWorker extends TaskBucketWorker {
    private final PoolTaskCenter pool;
    private final ReentrantLock lockOfHandles;
    private int countOfHandles;
    private PoolTimedTaskHandle<?>[] handles;

    public TaskScheduleWorker(PoolTaskCenter pool) {
        super(0L, false, 1);
        this.pool = pool;
        this.lockOfHandles = new ReentrantLock();
        this.handles = new PoolTimedTaskHandle<?>[0];
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods(4)                                               //
    //***************************************************************************************************************//
    public void put(PoolTaskHandle<?> taskHandle) {
        int insertPos = -1;
        taskHandle.setTaskBucket(this);
        PoolTimedTaskHandle<?> handle = (PoolTimedTaskHandle<?>) taskHandle;

        try {
            //acquire lock of array
            lockOfHandles.lock();

            //create a new array if full
            if (handles.length == countOfHandles) {
                int newCapacity = countOfHandles + (countOfHandles < 64 ? countOfHandles + 2 : countOfHandles >> 1);
                PoolTimedTaskHandle<?>[] newHandles = new PoolTimedTaskHandle<?>[newCapacity];
                System.arraycopy(handles, 0, newHandles, 0, countOfHandles);
                this.handles = newHandles;
            }

            //find out index to insert handle
            final int maxSeq = countOfHandles - 1;
            final long taskNextTime = handle.getNextTime();
            for (int i = maxSeq; i >= 0; i--) {//from tail to head
                if (taskNextTime >= handles[i].getNextTime()) {//found pos
                    insertPos = i + 1;
                    break;
                }
            }
            //move handles backward
            if (insertPos == -1) insertPos = 0;
            if (insertPos <= maxSeq)
                System.arraycopy(handles, insertPos, handles, insertPos + 1, countOfHandles - insertPos);

            //put handle to pos of array
            handles[insertPos] = handle;
            //increase count of tasks
            countOfHandles++;

            //if insertion pos is at first,then wake up work thread
            if (insertPos == 0) {
                if (this.state == WORKER_PASSIVATED) {
                    this.workThread = new Thread(this);
                    this.state = WORKER_RUNNING;
                    this.workThread.start();
                } else {
                    LockSupport.unpark(workThread);
                }
            }
        } finally {
            lockOfHandles.unlock();//unlock
        }
    }

    public List<PoolTaskHandle<?>> getUnCompletedTasks() {
        List<PoolTaskHandle<?>> allTasks = new LinkedList<>(Arrays.asList(handles));
        this.handles = new PoolTimedTaskHandle[0];
        this.countOfHandles = 0;
        LockSupport.unpark(workThread);
        return allTasks;
    }

    public void remove(PoolTaskHandle<?> taskHandle) {
        int pos = -1;
        try {
            lockOfHandles.lock();//lock of handles array
            final int maxSeq = countOfHandles - 1;
            for (int i = maxSeq; i >= 0; i--) {//from tail to head
                if (taskHandle == handles[i]) {
                    pos = i;

                    //move handles forward
                    System.arraycopy(handles, pos + 1, handles, pos, maxSeq - pos);
                    handles[maxSeq] = null;
                    this.countOfHandles--;
                    break;
                }
            }
        } finally {
            lockOfHandles.unlock();//unlock
        }

        if (pos == 0) LockSupport.unpark(workThread);
    }

    public boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning) {
        return false;
    }

    //***************************************************************************************************************//
    //                                            2: core method to process tasks                                    //
    //***************************************************************************************************************//
    public void run() {
        long parkTimeForFirstHandle;
        PoolTimedTaskHandle<?> firstHandle;

        do {
            firstHandle = null;
            parkTimeForFirstHandle = 0L;

            //1:poll out first hande if expired
            try {
                lockOfHandles.lock();
                if (countOfHandles > 0) {
                    parkTimeForFirstHandle = handles[0].getNextTime() - System.nanoTime();

                    if (parkTimeForFirstHandle <= 0L && pool.incrementExecTaskCount(1)) {
                        firstHandle = handles[0];
                        final int maxSeq = countOfHandles - 1;
                        System.arraycopy(handles, 1, handles, 0, maxSeq);//move forward
                        handles[maxSeq] = null;
                        this.countOfHandles--;
                    }
                }
            } finally {
                lockOfHandles.unlock();
            }

            //2: process handle if first handle is not null
            if (firstHandle != null) {
                if (firstHandle.isWaiting())
                    pool.pushToExecuteWorker(firstHandle, true);
                else {
                    pool.decrementTimedTaskCount();
                    pool.decrementExecTaskCount();
                }
            } else if (parkTimeForFirstHandle > 0L) {//park work thread with specified time
                LockSupport.parkNanos(parkTimeForFirstHandle);
            } else {//if no timed task,then park
                LockSupport.park();
            }
        } while (pool.getPoolState() == POOL_RUNNING);

        //set worker state to passivated
        this.state = WORKER_PASSIVATED;
    }
}



