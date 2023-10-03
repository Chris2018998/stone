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
import org.stone.beetp.pool.exception.TaskExecutionException;

import static org.stone.beetp.pool.TaskPoolConstants.TASK_CALL_EXCEPTION;
import static org.stone.beetp.pool.TaskPoolConstants.TASK_CALL_RESULT;

/**
 * Once Task Executor
 *
 * @author Chris Liao
 * @version 1.0
 */
final class OnceExecFactory extends TaskExecFactory {

    OnceExecFactory(TaskPoolImplement pool) {
        super(pool);
    }

    public void executeTask(BaseHandle handle) {
        try {
            //1: increment running count
            taskRunningCount.incrementAndGet();//need think of count exceeded?

            //2: execute callback
            BeeTask task = handle.getTask();
            BeeTaskCallback callback = handle.getCallback();
            if (callback != null) {
                try {
                    callback.beforeCall(handle);
                } catch (Throwable e) {
                    //do nothing
                }
            }

            //3: execute task
            try {
                handle.setDone(TASK_CALL_RESULT, task.call());
            } catch (Throwable e) {
                handle.setDone(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
            }
        } finally {
            taskRunningCount.decrementAndGet();
            taskCompletedCount.incrementAndGet();
        }
    }
}