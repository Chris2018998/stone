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
import org.stone.beetp.TreeLayerTask;

/**
 * Root Tree Task
 *
 * @author Chris Liao
 * @version 1.0
 */
public class RootExceptionTask implements TreeLayerTask<Integer> {
    public TreeLayerTask<Integer>[] getSubTasks() {
        return new TreeLayerTask[]{new LevelTask10(), new LevelTask11(), new LevelTask12()};
    }

    public Integer join(TaskHandle<Integer>[] subTaskHandles) throws Exception {
        int sum = 0;
        for (TaskHandle<Integer> handle : subTaskHandles) {
            sum += handle.get();
        }
        return sum;
    }
}

