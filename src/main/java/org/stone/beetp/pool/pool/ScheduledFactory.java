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

/**
 * Once Task Executor
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ScheduledFactory extends PlainExecFactory {
    private final ScheduledTaskQueue scheduledQueue;

    ScheduledFactory(TaskPoolImplement pool) {
        super(pool);
        scheduledQueue = pool.getScheduledDelayedQueue();
    }

    void afterExecute(PlainTaskHandle handle) {
        taskRunningCount.decrementAndGet();

        ScheduledTaskHandle scheduledHandle = (ScheduledTaskHandle) handle;
        if (scheduledHandle.isPeriodic()) {
            scheduledHandle.prepareForNextCall();//reset to waiting state for next execution
            if (scheduledQueue.add(scheduledHandle) == 0)
                pool.wakeupSchedulePeekThread();
        } else {//one timed task,so end
            taskCompletedCount.incrementAndGet();
        }
    }
}