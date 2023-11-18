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

import org.stone.beetp.Task;
import org.stone.beetp.TreeTask;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Task work thread
 *
 * @author Chris Liao
 * @version 1.0
 */
class TaskWorkThread extends Thread {
    //private queue of work thread for join tasks,can be steal by other work threads
    protected ConcurrentLinkedDeque<Task> joinTaskQueue;
    protected ConcurrentLinkedDeque<TreeTask> treeTaskQueue;

    //task handle in processing by this work thread
    volatile BaseHandle currentTaskHandle;

    void interrupt(BaseHandle taskHandle) {
        if (taskHandle == this.currentTaskHandle)
            this.interrupt();
    }
}
