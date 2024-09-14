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

import static org.stone.beetp.pool.PoolConstants.WORKER_INACTIVE;
import static org.stone.beetp.pool.PoolConstants.WORKER_RUNNING;

/**
 * Pool task schedule worker
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskScheduleWorker extends TaskBucketWorker {
    private final ReentrantLock lockOfHandles;
    private int countOfHandles;
    private PoolTimedTaskHandle<?>[] handles;

    public TaskScheduleWorker(PoolTaskCenter pool) {
        super(pool, 0L, false, 1);
        this.lockOfHandles = new ReentrantLock();
        this.handles = new PoolTimedTaskHandle<?>[0];
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods(4)                                               //
    //***************************************************************************************************************//
    public void put(PoolTaskHandle<?> taskHandle) {
        //1: insert given handle to array
        PoolTimedTaskHandle<?> handle = (PoolTimedTaskHandle<?>) taskHandle;
        int insertPos = -1;//insertion pos in array

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
        } finally {
            lockOfHandles.unlock();//unlock
        }

        //step2: if insertion index is zero,then wakeup work thread to pick it or wait it util expired
        if (insertPos == 0) {
            int curState = state;
            if (curState == WORKER_INACTIVE && StateUpd.compareAndSet(this, curState, WORKER_RUNNING)) {
                this.workThread = new Thread(this);
                this.workThread.start();
            } else {
                LockSupport.unpark(workThread);
            }
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
        lockOfHandles.lock();//lock of handles array
        try {
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

    public Object pollExpiredHandle() {
        lockOfHandles.lock();
        try {
            if (countOfHandles == 0) return -1L;
            PoolTimedTaskHandle<?> handle = handles[0];

            //if first handle not expired,then return the
            long remainTime = handle.getNextTime() - System.nanoTime();
            if (remainTime > 0) return remainTime;//nanoseconds

            final int maxSeq = countOfHandles - 1;
            System.arraycopy(handles, 1, handles, 0, maxSeq);
            handles[maxSeq] = null;
            this.countOfHandles--;

            return handle;
        } finally {
            lockOfHandles.unlock();
        }
    }

    public boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning) {
        return false;
    }

    //***************************************************************************************************************//
    //                                            2: core method to process tasks                                    //
    //***************************************************************************************************************//
    public void run() {//poll expired tasks and push them to execute workers
        do {
            //1: check worker state,if dead then exit from loop
            if (state == WORKER_INACTIVE) {
                this.workThread = null;
                break;
            }
            //2: clear interrupted flag of this worker thread if it exists
            if (workThread.isInterrupted() && Thread.interrupted()) {
                //no code here
            }

            //3: poll expired timed task
            Object polledObject = this.pollExpiredHandle();

            //4: if polled object is expired schedule task
            if (polledObject instanceof PoolTimedTaskHandle) {
                PoolTimedTaskHandle<?> taskHandle = (PoolTimedTaskHandle<?>) polledObject;
                if (taskHandle.isWaiting())
                    pool.pushToExecuteWorker(taskHandle);
                else
                    pool.getTaskCount().decrementAndGet();
            } else {
                Long time = (Long) polledObject;
                if (time > 0L) {
                    LockSupport.parkNanos(time);
                } else {
                    LockSupport.park();
                }
            }
        } while (true);
    }
}



