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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Once Task Executor
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinExecFactory extends TaskExecFactory {

    JoinExecFactory(TaskPoolImplement pool) {
        super(pool);
    }

    void execute(BaseHandle handle) {
        JoinTaskHandle joinHandle = (JoinTaskHandle) handle;
        if (joinHandle.isRoot()) beforeExecute(handle);

        //2: try to split task to children tasks
        BeeTaskJoinOperator joinOperator = ((JoinTaskHandle) handle).getJoinOperator();
        List<BeeTask> childTasks = joinOperator.split(handle.getTask());

        //3: create sub children and push them to queue
        if (childTasks != null && !childTasks.isEmpty()) {
            AtomicInteger completedCount = new AtomicInteger(childTasks.size());
            ArrayList<JoinTaskHandle> childList = new ArrayList<>(childTasks.size());
            for (BeeTask childTask : childTasks) {
                JoinTaskHandle childHandle = new JoinTaskHandle(childTask, joinHandle, completedCount, joinOperator, pool);
                pool.pushToExecutionQueue(childHandle);
                childList.add(childHandle);
            }
            joinHandle.setChildrenList(childList);
        } else if (joinHandle.isRoot()) {//execute the root task
            executeTask(handle);
            afterExecute(handle);
        }
    }
}