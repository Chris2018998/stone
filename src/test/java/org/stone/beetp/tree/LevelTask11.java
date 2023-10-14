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

import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTreeTask;

public class LevelTask11 implements BeeTreeTask<Integer> {

    public BeeTreeTask<Integer>[] getSubTasks() {
        return new BeeTreeTask[]{new LevelTask1103(), new LevelTask1104()};
    }

    public Integer call(BeeTaskHandle<Integer>[] subTaskHandles) throws Exception {
        int sum = 0;
        for (BeeTaskHandle<Integer> handle : subTaskHandles) {
            sum += handle.get();
        }
        return sum;
    }
}
