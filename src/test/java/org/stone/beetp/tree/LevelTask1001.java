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

public class LevelTask1001 implements BeeTreeTask<Integer> {

    public BeeTreeTask<Integer>[] getSubTasks() {
        return null;
    }

    public Integer call(BeeTaskHandle<Integer>[] subTaskHandles) throws Exception {
        return new Integer(1);
    }
}