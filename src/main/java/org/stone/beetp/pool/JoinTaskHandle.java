/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

import org.stone.beetp.Task;
import org.stone.beetp.TaskAspect;
import org.stone.beetp.TaskJoinOperator;
import org.stone.beetp.pool.exception.TaskCountExceededException;
import org.stone.beetp.pool.exception.TaskExecutionException;
import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.stone.beetp.pool.PoolConstants.TASK_FAILED;
import static org.stone.beetp.pool.PoolConstants.TASK_SUCCEED;

/**
 * join task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinTaskHandle<V> extends PoolTaskHandle<V> {
    private static final AtomicIntegerFieldUpdater<JoinTaskHandle> exceptionIndUpd = IntegerFieldUpdaterImpl.newUpdater(JoinTaskHandle.class, "exceptionInd");
    private static final AtomicIntegerFieldUpdater<JoinTaskHandle> unCompletedCountUpd = IntegerFieldUpdaterImpl.newUpdater(JoinTaskHandle.class, "subTaskHandleCount");

    private final JoinTaskHandle<V> root;
    private final JoinTaskHandle<V> parent;
    private final TaskJoinOperator<V> operator;
    private volatile int exceptionInd;
    private volatile int subTaskHandleCount;
    private JoinTaskHandle<V>[] subTaskHandles;

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    JoinTaskHandle(Task<V> task, TaskJoinOperator<V> operator, TaskAspect<V> callback, PoolTaskCenter pool) {//root task
        super(task, callback, pool, true);
        this.operator = operator;
        this.root = this;
        this.parent = null;
    }

    private JoinTaskHandle(Task<V> task, JoinTaskHandle<V> parent, JoinTaskHandle<V> root, PoolTaskCenter pool) {//for sub tasks
        super(task, null, pool, false);
        this.root = root;
        this.parent = parent;
        this.operator = null;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (subTaskHandles != null) {
            if (this == root) {
                new AsynJoinCancelThread<V>(root.subTaskHandles, mayInterruptIfRunning).start();
            } else {
                for (JoinTaskHandle childHandle : subTaskHandles)
                    childHandle.cancel(mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    //***************************************************************************************************************//
    //                                          4: execute task                                                      //
    //***************************************************************************************************************//
    protected void executeTask(TaskExecutionWorker execWorker) {
        Task<V>[] subTasks = root.operator.split(this.task);
        int splitChildCount = subTasks != null ? subTasks.length : 0;

        if (splitChildCount > 0) {
            pool.decrementTaskCount();
            execWorker.incrementCompletedCount();

            if (pool.incrementTaskCountForInternal(splitChildCount)) {
                this.subTaskHandles = new JoinTaskHandle[splitChildCount];
                this.subTaskHandleCount = splitChildCount;
                for (int i = 0; i < splitChildCount; i++)
                    subTaskHandles[i] = new JoinTaskHandle<>(subTasks[i], this, root, pool);

                execWorker.getTaskBucket().addAll(Arrays.asList(subTaskHandles));
                pool.attemptActivateAllWorkers();
            } else {
                this.handleSubTaskException(new TaskExecutionException(new TaskCountExceededException("Task count exceeded")));
            }
        } else {
            super.executeTask(execWorker);
        }
    }

    //***************************************************************************************************************//
    //                                  5: result method                                                             //
    //***************************************************************************************************************//
    protected void afterExecute(boolean successful, Object result) {
        if (parent == null) return;

        if (successful) {
            do {
                int currentSize = parent.subTaskHandleCount;
                if (currentSize == 0) break;
                if (unCompletedCountUpd.compareAndSet(parent, currentSize, currentSize - 1)) {
                    if (currentSize == 1) {
                        try {
                            parent.fillTaskResult(TASK_SUCCEED, root.operator.join(parent.subTaskHandles));
                        } catch (Throwable e) {
                            this.handleSubTaskException(new TaskExecutionException(e));
                        }
                    }
                    break;
                }
            } while (true);
        } else {
            this.handleSubTaskException(result);
        }
    }

    private void handleSubTaskException(Object result) {
        if (exceptionIndUpd.compareAndSet(root, 0, 1)) {
            root.fillTaskResult(TASK_FAILED, result);
            new AsynJoinCancelThread<V>(root.subTaskHandles, true).start();
        }
    }

    private static class AsynJoinCancelThread<V> extends Thread {
        private final boolean mayInterruptIfRunning;
        private final JoinTaskHandle<V>[] subTaskHandles;

        AsynJoinCancelThread(JoinTaskHandle<V>[] subTaskHandles, boolean mayInterruptIfRunning) {
            this.subTaskHandles = subTaskHandles;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
        }

        public void run() {
            for (JoinTaskHandle<V> childHandle : subTaskHandles)
                childHandle.cancel(mayInterruptIfRunning);
        }
    }
}
