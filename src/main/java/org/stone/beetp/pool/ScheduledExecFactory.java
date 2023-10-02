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

import org.stone.beetp.BeeTaskHandle;

/**
 * Once Task Executor
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ScheduledExecFactory extends TaskExecFactory {

    ScheduledExecFactory(TaskPoolImplement pool) {
        super(pool);
    }

    public void executeTask(BeeTaskHandle handle) {

    }
}