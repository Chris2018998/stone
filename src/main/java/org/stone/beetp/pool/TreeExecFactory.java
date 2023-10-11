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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beetp.BeeTaskStates.TASK_CALL_EXCEPTION;
import static org.stone.beetp.BeeTaskStates.TASK_CALL_RESULT;

/**
 * tree Execute Factory
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
        List<BeeTreeTask> childTasks = treeTask.childrenList();

        //3: create sub children and push them to queue
        if (childTasks != null && !childTasks.isEmpty()) {
            int childrenSize = childTasks.size();
            TreeTaskHandle root = handle.getRoot();
            if (root == null) root = handle;
            AtomicInteger completedCount = new AtomicInteger();
            ArrayList<TreeTaskHandle> childList = new ArrayList<>(childrenSize);
            for (BeeTreeTask childTask : childTasks) {
                TreeTaskHandle childHandle = new TreeTaskHandle(childTask, handle, childrenSize, completedCount, pool, root);
                pool.pushToExecutionQueue(childHandle);
                childList.add(childHandle);
            }
            handle.setChildrenList(childList);
        } else {//execute leafed task
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