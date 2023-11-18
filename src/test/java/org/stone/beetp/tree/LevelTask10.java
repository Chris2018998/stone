/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.tree;

import org.stone.beetp.TaskHandle;
import org.stone.beetp.TreeTask;

public class LevelTask10 implements TreeTask<Integer> {

    public TreeTask<Integer>[] getSubTasks() {
        return new TreeTask[]{new LevelTask1001(), new LevelTask1002()};
    }

    public Integer call(TaskHandle<Integer>[] subTaskHandles) throws Exception {
        int sum = 0;
        for (TaskHandle<Integer> handle : subTaskHandles) {
            sum += handle.get();
        }
        return sum;
    }
}
