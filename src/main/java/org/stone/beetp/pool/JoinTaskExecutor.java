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

import org.stone.beetp.BeeTask;
import org.stone.beetp.BeeTaskJoinOperator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Join task Executor
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinTaskExecutor extends PlainTaskExecutor {

    JoinTaskExecutor(TaskPoolImplement pool) {
        super(pool);
    }

    void execute(PlainTaskHandle handle) {
        JoinTaskHandle joinHandle = (JoinTaskHandle) handle;
        if (joinHandle.isRoot()) beforeExecute(handle);

        //2: try to split task to children tasks
        BeeTaskJoinOperator joinOperator = joinHandle.getJoinOperator();
        BeeTask[] subTasks = joinOperator.split(joinHandle.getTask());

        //3: create sub children and push them to queue
        if (subTasks != null && subTasks.length > 0) {
            int subSize = subTasks.length;
            AtomicInteger completedCount = new AtomicInteger();
            JoinTaskHandle[] subJoinHandles = new JoinTaskHandle[subSize];
            JoinTaskHandle root = joinHandle.isRoot() ? joinHandle : joinHandle.getRoot();
            joinHandle.setSubTaskHandles(subJoinHandles);

            for (int i = 0; i < subSize; i++) {
                subJoinHandles[i] = new JoinTaskHandle(subTasks[i], joinHandle, subSize, completedCount, joinOperator, pool, root);
                pool.pushToExecutionQueue(subJoinHandles[i]);
            }
        } else {//execute leaf task
            executeTask(handle);
        }
    }
}