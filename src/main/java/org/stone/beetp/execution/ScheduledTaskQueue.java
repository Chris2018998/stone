/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.execution;

import org.stone.tools.SortedArray;

import java.util.Comparator;

/**
 * Task Scheduled Queue
 *
 * @author Chris Liao
 * @version 1.0
 */

final class ScheduledTaskQueue extends SortedArray<ScheduledTaskHandle> {
    ScheduledTaskQueue(int initSize) {
        super(ScheduledTaskHandle.class, initSize, new Comparator<ScheduledTaskHandle>() {
            public int compare(ScheduledTaskHandle handle1, ScheduledTaskHandle handle2) {
                long compareV = handle1.getNextTime() - handle2.getNextTime();
                if (compareV < 0) return -1;
                if (compareV == 0) return 0;
                return 1;
            }
        });
    }

    Object pollExpired() {
        arrayLock.lock();
        try {
            //1: empty queue,return -1
            if (count == 0) return -1L;
            ScheduledTaskHandle handle = objects[0];

            //2:if first task not be expired,return remain time
            long remainTime = handle.getNextTime() - System.nanoTime();
            if (remainTime > 0) return remainTime;//nanoseconds

            //3:first task expired,then remove it and return it
            this.remove(handle);
            return handle;
        } finally {
            arrayLock.unlock();
        }
    }

    ScheduledTaskHandle[] clearAll() {
        arrayLock.lock();
        try {
            ScheduledTaskHandle[] tasks = objects;
            this.objects = new ScheduledTaskHandle[0];
            this.count = 0;
            return tasks;
        } finally {
            arrayLock.unlock();
        }
    }
}
