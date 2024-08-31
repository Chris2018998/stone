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

import org.stone.shine.util.concurrent.locks.ReentrantLock;

import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

import static org.stone.beetp.pool.PoolConstants.POOL_CLEARING;
import static org.stone.beetp.pool.PoolConstants.POOL_RUNNING;

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
        super(pool);
        this.lockOfHandles = new ReentrantLock();
        this.handles = new PoolTimedTaskHandle<?>[0];
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods(4)                                               //
    //***************************************************************************************************************//
    public void put(PoolTaskHandle<?> taskHandle) {
        PoolTimedTaskHandle<?> handle = (PoolTimedTaskHandle<?>) taskHandle;

        int insertPos = -1;//insertion pos of given handle
        lockOfHandles.lock();//lock of handles array
        try {
            //grow array of tasks
            if (handles.length == countOfHandles) this.growArray();
            final long taskNextTime = handle.getNextTime();

            final int maxSeq = countOfHandles - 1;
            for (int i = maxSeq; i >= 0; i--) {//from tail to head
                if (taskNextTime >= handles[i].getNextTime()) {//found pos
                    insertPos = i + 1;
                    break;
                }
            }

            if (insertPos == -1) insertPos = 0;
            if (insertPos <= maxSeq)//copy of backward move
                System.arraycopy(handles, insertPos, handles, insertPos + 1, countOfHandles - insertPos);

            handles[insertPos] = handle;
            countOfHandles++;
        } finally {
            lockOfHandles.unlock();//unlock
        }

        //if insertion pos is zero,then wakeup worker thread to re-pick on new head
        if (insertPos == 0) LockSupport.unpark(workThread);
    }

    public PoolTimedTaskHandle<?>[] clearAll() {
        lockOfHandles.lock();
        try {
            PoolTimedTaskHandle<?>[] tasks = handles;
            this.handles = new PoolTimedTaskHandle[0];
            this.countOfHandles = 0;
            return tasks;
        } finally {
            lockOfHandles.unlock();
        }
    }

    public int remove(PoolTaskHandle<?> taskHandle) {
        int pos = -1;
        lockOfHandles.lock();//lock of handles array
        try {
            final int maxSeq = countOfHandles - 1;
            for (int i = maxSeq; i >= 0; i--) {//from tail to head
                if (taskHandle == handles[i]) {
                    pos = i;
                    //copy of front move
                    System.arraycopy(handles, pos + 1, handles, pos, maxSeq - pos);
                    handles[maxSeq] = null;
                    this.countOfHandles--;
                    break;
                }
            }
            return pos;
        } finally {
            lockOfHandles.unlock();//unlock
        }
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

    private void growArray() {
        int oldCapacity = handles.length;
        int newCapacity = oldCapacity + (oldCapacity < 64 ? oldCapacity + 2 : oldCapacity >> 1);
        this.handles = Arrays.copyOf(handles, newCapacity);
    }

    public boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning) {
        return false;

    }

    //***************************************************************************************************************//
    //                                            2: core method to process tasks                                    //
    //***************************************************************************************************************//
    public void run() {//poll expired tasks and push them to execute workers
        while (true) {
            int poolCurState = pool.getPoolState();
            if (poolCurState == POOL_RUNNING) {

                //1: poll expired task
                Object polledObject = this.pollExpiredHandle();

                //2: if polled object is expired schedule task
                if (polledObject instanceof PoolTimedTaskHandle) {
                    PoolTimedTaskHandle<?> taskHandle = (PoolTimedTaskHandle<?>) polledObject;
                    if (taskHandle.isWaiting())
                        pool.pushToExecutionQueue(taskHandle);
                    else
                        pool.getTaskCount().decrement();
                } else {//3: the polled object is time,then park
                    Long time = (Long) polledObject;
                    if (time > 0L) {
                        LockSupport.parkNanos(time);
                    } else {
                        LockSupport.park();
                    }
                }
            }

            //4: pool state check,if in clearing,then park peek thread
            if (poolCurState == POOL_CLEARING) LockSupport.park();
            if (poolCurState > POOL_CLEARING) break;
        }
    }
}



