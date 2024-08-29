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
import java.util.Comparator;

/**
 * Pool task schedule worker
 *
 * @author Chris Liao
 * @version 1.0
 */

public class TaskScheduleWorker implements PoolTaskBucket, Runnable {
    private final Thread workThread;
    private final PoolTaskCenter pool;
    private final ReentrantLock lockOfHandles;
    private final ExecuteTimeComparator timeComparator;
    private int countOfHandles;
    private PoolTimedTaskHandle<?>[] handles;

    public TaskScheduleWorker(PoolTaskCenter pool) {
        this.pool = pool;
        this.lockOfHandles = new ReentrantLock();
        this.handles = new PoolTimedTaskHandle<?>[0];
        this.timeComparator = new ExecuteTimeComparator();

        this.workThread = new Thread(this);//@todo created by thread factory
        this.workThread.start();
    }

    //***************************************************************************************************************//
    //                                            1: bucket methods                                                  //
    //***************************************************************************************************************//
    public void put(PoolTaskHandle<?> taskHandle) {
        PoolTimedTaskHandle<?> handle = (PoolTimedTaskHandle<?>) taskHandle;

        lockOfHandles.lock();//lock array
        try {
            //1:if full,then grown
            if (handles.length == countOfHandles) this.growArray();

            int pos = -1;//insertion pos of given handle
            if (countOfHandles > 0) {
                for (int i = countOfHandles - 1; i >= 0; i--) {
                    if (timeComparator.compare(handle, handles[i]) >= 0) {
                        pos = i + 1;

                        System.arraycopy(this.handles, pos, handles, pos + 1, countOfHandles - pos);
                        handles[pos] = handle;
                        break;
                    }
                }

                //All elements move backward
                if (pos == -1) System.arraycopy(this.handles, 0, handles, 1, countOfHandles);
            }

            if (pos == -1) handles[++pos] = handle;

            countOfHandles++;

            //return pos;
        } finally {
            lockOfHandles.unlock();//unlock
        }
    }

    public Object pollExpired() {
        lockOfHandles.lock();
        try {
            //1: empty queue,return -1
            if (countOfHandles == 0) return -1L;
            PoolTimedTaskHandle<?> handle = handles[0];

            //2:if first task not be expired,return remain time
            long remainTime = handle.getNextTime() - System.nanoTime();
            if (remainTime > 0) return remainTime;//nanoseconds

            //3:first task expired,then remove it and return it
            //this.remove(handle);
            return handle;
        } finally {
            lockOfHandles.unlock();
        }
    }

    PoolTimedTaskHandle<?>[] clearAll() {
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


    public boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning) {
        return false;

    }

    private void growArray() {
        int oldCapacity = handles.length;
        int newCapacity = oldCapacity + (oldCapacity < 64 ?
                oldCapacity + 2 :
                oldCapacity >> 1);
        this.handles = Arrays.copyOf(handles, newCapacity);
    }


    //***************************************************************************************************************//
    //                                            2: core method to process tasks                                    //
    //***************************************************************************************************************//
    public void run() {//poll expired tasks and push them to execute workers
//        while (true) {
//            int poolCurState = poolState;
//            if (poolCurState == POOL_RUNNING) {
//                //1: poll expired task
//
//                Object polledObject = scheduledDelayedQueue.pollExpired();
//
//                //2: if polled object is expired schedule task
//                if (polledObject instanceof ScheduledTaskHandle) {
//                    ScheduledTaskHandle taskHandle = (ScheduledTaskHandle) polledObject;
//                    if (taskHandle.getState() == TASK_WAITING)
//                        pool.pushToExecutionQueue(taskHandle);//push it to execution queue
//                    else
//                        taskCount.decrementAndGet();//task has cancelled,so remove it
//                } else {//3: the polled object is time,then park
//                    Long time = (Long) polledObject;
//                    if (time > 0) {
//                        LockSupport.parkNanos(time);
//                    } else {
//                        LockSupport.park();
//                    }
//                }
//            }
//
//            //4: pool state check,if in clearing,then park peek thread
//            if (poolCurState == POOL_CLEARING) LockSupport.park();
//            if (poolCurState > POOL_CLEARING) break;
//        }
    }

    private static class ExecuteTimeComparator implements Comparator<PoolTimedTaskHandle<?>> {
        public int compare(PoolTimedTaskHandle handle1, PoolTimedTaskHandle handle2) {
            long compareV = handle1.getNextTime() - handle2.getNextTime();
            if (compareV < 0) return -1;
            if (compareV == 0) return 0;
            return 1;
        }
    }
}



