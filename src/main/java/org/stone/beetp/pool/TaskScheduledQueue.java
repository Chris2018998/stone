/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.pool;

import org.stone.util.SortedArray;

import java.util.Comparator;

/**
 * Task Scheduled Queue
 *
 * @author Chris Liao
 * @version 1.0
 */

final class TaskScheduledQueue extends SortedArray<TaskScheduledHandle> {

    TaskScheduledQueue(int initSize, Comparator<TaskScheduledHandle> comparator) {
        super(TaskScheduledHandle.class, initSize, comparator);
    }

    //1: if first task expired,then return it; 2: if not expired,return remain time 3: if array is empty,return 0;
    Object pollExpired() {
        arrayLock.lock();
        try {
            if (count == 0) return -1L;
            TaskScheduledHandle handle = objects[0];
            long remainTime = handle.getNextTime() - System.nanoTime();
            if (remainTime > 0) return remainTime;//nanoseconds

            //expired,then remove it
            this.remove(handle);
            return handle;
        } finally {
            arrayLock.unlock();
        }
    }
}
