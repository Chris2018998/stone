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

import org.stone.beetp.BeeTaskCallback;
import org.stone.beetp.pool.exception.TaskExecutionException;

import java.util.concurrent.atomic.AtomicLong;

import static org.stone.beetp.BeeTaskStates.TASK_CALL_EXCEPTION;
import static org.stone.beetp.BeeTaskStates.TASK_CALL_RESULT;

/**
 * Task Execute Factory
 *
 * @author Chris Liao
 * @version 1.0
 */
class PlainExecFactory {
    final TaskPoolImplement pool;
    final AtomicLong taskRunningCount;
    final AtomicLong taskCompletedCount;

    PlainExecFactory(TaskPoolImplement pool) {
        this.pool = pool;
        this.taskRunningCount = pool.getTaskRunningCount();
        this.taskCompletedCount = pool.getTaskCompletedCount();
    }

    //***************************************************************************************************************//
    //                                      1: execution                                                             //
    //***************************************************************************************************************//
    void execute(PlainTaskHandle handle) {
        beforeExecute(handle);
        executeTask(handle);
        afterExecute(handle);
    }

    //***************************************************************************************************************//
    //                                      2: interceptor methods(3)                                                //
    //***************************************************************************************************************//

    void beforeExecute(PlainTaskHandle handle) {
        taskRunningCount.incrementAndGet();//need think of count exceeded?
    }

    void executeTask(PlainTaskHandle handle) {
        BeeTaskCallback callback = handle.getCallback();
        if (callback != null) {
            try {
                callback.beforeCall(handle);
            } catch (Throwable e) {
                //do nothing
            }
        }

        try {
            handle.setResult(TASK_CALL_RESULT, handle.getTask().call());
        } catch (Throwable e) {
            handle.setResult(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
        }
    }

    void afterExecute(PlainTaskHandle handle) {
        taskRunningCount.decrementAndGet();
        taskCompletedCount.incrementAndGet();
    }
}
