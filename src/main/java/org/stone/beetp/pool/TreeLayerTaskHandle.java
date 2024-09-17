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

import org.stone.beetp.TaskAspect;
import org.stone.beetp.TreeLayerTask;
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
final class TreeLayerTaskHandle<V> extends PoolTaskHandle<V> {
    //3: fields of child task
    private final TreeLayerTask<V> task;
    TreeLayerTaskHandle<V> root;
    //1: field of root
    private AtomicBoolean exceptionInd;
    //2: field of parent
    private TreeLayerTaskHandle<V>[] subTaskHandles;
    private TreeLayerTaskHandle<V> parent;
    private AtomicInteger completedDownOfSubTask;//the complete count of sub tasks.

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    TreeLayerTaskHandle(TreeLayerTask<V> task, final TaskAspect<V> callback, PoolTaskCenter pool) {
        super(null, callback, pool, true);
        this.task = task;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    private TreeLayerTaskHandle(TreeLayerTask<V> task, TreeLayerTaskHandle<V> parent, AtomicInteger countDown, PoolTaskCenter pool, TreeLayerTaskHandle<V> root) {
        super(null, null, pool, false);
        this.task = task;
        this.root = root;
        this.parent = parent;
        this.completedDownOfSubTask = countDown;
    }

    TreeLayerTask getTreeLayerTask() {
        return task;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (subTaskHandles != null) {
            if (this.isRoot()) {
                new AsynTreeCancelThread(subTaskHandles, mayInterruptIfRunning).start();
            } else {
                for (TreeLayerTaskHandle<V> childHandle : subTaskHandles)
                    childHandle.cancel(mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    //***************************************************************************************************************//
    //                                          4: execute task                                                      //
    //***************************************************************************************************************//
    private void beforeExecute() {
    }

    private void afterExecute(TaskExecuteWorker worker) {
    }

    private Object invokeTaskCall() throws Exception {
        return task.join(null);
    }

    private void executeTask(TaskExecuteWorker worker) {
        //2: try to split current task into subtasks
        TreeLayerTask<V>[] subTasks = this.task.getSubTasks();

        //3: push sub tasks to execute queue
        if (subTasks != null && subTasks.length > 0) {
            AtomicInteger completedDownOfSubTask = new AtomicInteger(subTasks.length);
            List<PoolTaskHandle<?>> handleList = new LinkedList<>();
            for (TreeLayerTask<V> subTask : subTasks)
                handleList.add(new TreeLayerTaskHandle<V>(subTask, this, completedDownOfSubTask, pool, root));

            ((TaskExecuteWorker) this.state).put(handleList);
        } else {//4: execute leaf task
            super.executeTask();
        }
    }

    //***************************************************************************************************************//
    //                              4: task result                                                                   //                                                                                  //
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
                            parent.fillTaskResult(TASK_SUCCEED, parent.task.join(parent.subTaskHandles));//join children
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

            new AsynTreeCancelThread<V>(root.subTaskHandles, true).start();
        }
    }

    private static class AsynTreeCancelThread<V> extends Thread {
        private final boolean mayInterruptIfRunning;
        private final TreeLayerTaskHandle<V>[] subTaskHandles;

        AsynTreeCancelThread(TreeLayerTaskHandle<V>[] subTaskHandles, boolean mayInterruptIfRunning) {
            this.subTaskHandles = subTaskHandles;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
        }

        public void run() {
            for (TreeLayerTaskHandle<V> childHandle : subTaskHandles)
                childHandle.cancel(mayInterruptIfRunning);
        }
    }
}
