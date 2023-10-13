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

import org.stone.beetp.BeeTaskCallback;
import org.stone.beetp.BeeTreeTask;
import org.stone.beetp.pool.exception.TaskExecutionException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beetp.BeeTaskStates.TASK_CALL_EXCEPTION;
import static org.stone.beetp.BeeTaskStates.TASK_CALL_RESULT;

/**
 * tree task Execute Factory
 *
 * @author Chris Liao
 * @version 1.0
 */
final class TreeExecFactory extends BaseExecFactory {

    TreeExecFactory(TaskPoolImplement pool) {
        super(pool);
    }

    void execute(TreeTaskHandle handle) {
        if (handle.isRoot()) taskRunningCount.incrementAndGet();

        //1: try to split task to children tasks
        BeeTreeTask treeTask = handle.getTask();
        BeeTreeTask[] subTasks = treeTask.getSubTasks();

        //3: create sub children and push them to queue
        if (subTasks != null && subTasks.length > 0) {
            int subSize = subTasks.length;
            AtomicInteger completedCount = new AtomicInteger();
            TreeTaskHandle[] subTaskHandles = new TreeTaskHandle[subSize];
            TreeTaskHandle root = handle.isRoot() ? handle : handle.getRoot();
            handle.setSubTaskHandles(subTaskHandles);

            for (int i = 0; i < subSize; i++) {
                subTaskHandles[i] = new TreeTaskHandle(subTasks[i], handle, subSize, completedCount, pool, root);
                pool.pushToExecutionQueue(subTaskHandles[i]);
            }
        } else {//execute leaf task
            BeeTaskCallback callback = handle.getCallback();
            if (callback != null) {
                try {
                    callback.beforeCall(handle);
                } catch (Throwable e) {
                    //do nothing
                }
            }

            try {
                handle.setResult(TASK_CALL_RESULT, handle.getTask().call(null));
            } catch (Throwable e) {
                handle.setResult(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
            }
        }
    }
}