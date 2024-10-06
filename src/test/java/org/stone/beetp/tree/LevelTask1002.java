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

public class LevelTask1002 implements TreeLayerTask<Integer> {

    public TreeLayerTask<Integer>[] getSubTasks() {
        return null;
    }

    public Integer join(TaskHandle<Integer>[] subTaskHandles) throws Exception {
        return new Integer(2);
    }
}