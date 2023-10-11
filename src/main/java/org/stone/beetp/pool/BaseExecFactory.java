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

import java.util.concurrent.atomic.AtomicLong;

/**
 * base factory
 *
 * @author Chris Liao
 * @version 1.0
 */
class BaseExecFactory {
    final TaskPoolImplement pool;
    final AtomicLong taskRunningCount;
    final AtomicLong taskCompletedCount;

    BaseExecFactory(TaskPoolImplement pool) {
        this.pool = pool;
        this.taskRunningCount = pool.getTaskRunningCount();
        this.taskCompletedCount = pool.getTaskCompletedCount();
    }
}
