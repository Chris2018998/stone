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
 * Join Execute Factory
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinFactory extends PlainExecFactory {

    JoinFactory(TaskPoolImplement pool) {
        super(pool);
    }

    void execute(PlainTaskHandle handle) {
        JoinTaskHandle joinHandle = (JoinTaskHandle) handle;
        if (joinHandle.isRoot()) beforeExecute(handle);

        //2: try to split task to children tasks
        BeeTaskJoinOperator joinOperator = joinHandle.getJoinOperator();
        List<BeeTask> childTasks = joinOperator.split(joinHandle.getTask());

        //3: create sub children and push them to queue
        if (childTasks != null && !childTasks.isEmpty()) {
            int childrenSize = childTasks.size();
            JoinTaskHandle root = joinHandle.getRoot();
            if (root == null) root = joinHandle;
            AtomicInteger completedCount = new AtomicInteger();
            ArrayList<JoinTaskHandle> childList = new ArrayList<>(childrenSize);
            for (BeeTask childTask : childTasks) {
                JoinTaskHandle childHandle = new JoinTaskHandle(childTask, joinHandle, childrenSize, completedCount, joinOperator, pool, root);
                pool.pushToExecutionQueue(childHandle);
                childList.add(childHandle);
            }
            joinHandle.setChildrenList(childList);
        } else {//execute leafed task
            executeTask(handle);
        }
    }
}