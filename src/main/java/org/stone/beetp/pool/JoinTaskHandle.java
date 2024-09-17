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
import org.stone.beetp.pool.exception.TaskExecutionException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beetp.pool.PoolConstants.TASK_FAILED;
import static org.stone.beetp.pool.PoolConstants.TASK_SUCCEED;

/**
 * join task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinTaskHandle<V> extends PoolTaskHandle<V> {
    private final TaskJoinOperator<V> operator;
    JoinTaskHandle<V> root;

    private JoinTaskHandle<V> parent;
    private AtomicBoolean exceptionInd;

    private JoinTaskHandle<V>[] subTaskHandles;
    private AtomicInteger completedDownOfSubTask;//the complete count of subtasks.

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    JoinTaskHandle(Task<V> task, TaskJoinOperator<V> operator, TaskAspect<V> callback, PoolTaskCenter pool) {
        super(task, callback, pool, true);
        this.operator = operator;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    private JoinTaskHandle(Task<V> task, JoinTaskHandle<V> parent, AtomicInteger countDown, TaskJoinOperator<V> operator, PoolTaskCenter pool, JoinTaskHandle<V> root) {
        super(task, null, pool, false);
        this.root = root;
        this.parent = parent;
        this.operator = operator;
        this.completedDownOfSubTask = countDown;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (subTaskHandles != null) {
            if (this.isRoot()) {
                new AsynJoinCancelThread<>(root.subTaskHandles, mayInterruptIfRunning).start();
            } else {
                for (JoinTaskHandle<V> childHandle : subTaskHandles)
                    childHandle.cancel(mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    //***************************************************************************************************************//
    //                                          4: execute task                                                      //
    //***************************************************************************************************************//
    protected void executeTask() {
        Task<V>[] subTasks = operator.split(this.task);
        if (subTasks != null && subTasks.length > 0) {
            JoinTaskHandle<V> root = isRoot() ? this : this.root;
            List<PoolTaskHandle<?>> handleList = new LinkedList<>();
            AtomicInteger completedDownOfSubTask = new AtomicInteger(subTasks.length);

            for (Task<V> subTask : subTasks)
                handleList.add(new JoinTaskHandle<>(subTask, this, completedDownOfSubTask, operator, pool, root));

            ((TaskExecuteWorker) this.state).put(handleList);
        } else {//4: execute leaf task
            super.executeTask();
        }
    }

    //***************************************************************************************************************//
    //                                  5: result method                                                             //
    //***************************************************************************************************************//
    protected void afterExecute(boolean successful, Object result) {
        if (completedDownOfSubTask == null) return;

        if (successful) {
            do {
                int currentSize = completedDownOfSubTask.get();
                if (currentSize == 0) break;
                if (completedDownOfSubTask.compareAndSet(currentSize, currentSize - 1)) {
                    if (currentSize == 1) {
                        try {
                            parent.fillTaskResult(TASK_SUCCEED, operator.join(parent.subTaskHandles));//join children
                            if (parent.isRoot()) {
                                pool.getTaskCount().decrementAndGet();
                                // ((TaskExecuteWorker) Thread.currentThread()).incrementCompletedCount();
                            }
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
        if (root.exceptionInd.compareAndSet(false, true)) {
            root.fillTaskResult(TASK_FAILED, result);
            pool.getTaskCount().decrementAndGet();
            //((TaskExecuteWorker) Thread.currentThread()).incrementCompletedCount();

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
