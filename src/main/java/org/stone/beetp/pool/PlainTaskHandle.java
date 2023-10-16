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
import org.stone.beetp.BeeTaskCallback;

/**
 * Plain Handle
 *
 * @author Chris Liao
 * @version 1.0
 */
class PlainTaskHandle extends BaseHandle {
    private final BeeTask task;

    PlainTaskHandle(BeeTask task, final BeeTaskCallback callback, final boolean isRoot, final TaskPoolImplement pool) {
        super(isRoot, callback, pool);
        this.task = task;
    }

    BeeTask getTask() {
        return this.task;
    }
}
