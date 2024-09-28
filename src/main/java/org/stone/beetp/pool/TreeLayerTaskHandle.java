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
final class TreeLayerTaskHandle<V> extends PoolTaskHandle<V> {
    private static final AtomicIntegerFieldUpdater<TreeLayerTaskHandle> exceptionIndUpd = IntegerFieldUpdaterImpl.newUpdater(TreeLayerTaskHandle.class, "exceptionInd");
    private static final AtomicIntegerFieldUpdater<TreeLayerTaskHandle> unCompletedCountUpd = IntegerFieldUpdaterImpl.newUpdater(TreeLayerTaskHandle.class, "subTaskHandleCount");

    private final TreeLayerTaskHandle<V> root;
    private final TreeLayerTaskHandle<V> parent;
    private final TreeLayerTask<V> task;
    private volatile int exceptionInd;
    private volatile int subTaskHandleCount;
    private TreeLayerTaskHandle<V>[] subTaskHandles;

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    TreeLayerTaskHandle(TreeLayerTask<V> task, final TaskAspect<V> callback, PoolTaskCenter pool) {
        super(null, callback, pool, true);
        this.task = task;
        this.root = this;
        this.parent = null;
    }

    private TreeLayerTaskHandle(TreeLayerTask<V> task, TaskExecuteWorker bucketWorker, TreeLayerTaskHandle<V> parent, TreeLayerTaskHandle<V> root, PoolTaskCenter pool) {//for sub tasks
        super(null, null, pool, false);
        this.task = task;
        this.root = root;
        this.parent = parent;
        this.taskBucket = bucketWorker;
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
            if (this == root) {
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
    private Object invokeTaskCall() throws Exception {
        return task.join(null);
    }

    protected void executeTask(TaskExecuteWorker execWorker) {
        TreeLayerTask<V>[] subTasks = this.task.getSubTasks();
        int splitChildCount = subTasks != null ? subTasks.length : 0;

        if (splitChildCount > 0) {
            if (pool.incrementInternalTaskCount(subTaskHandleCount)) {
                this.subTaskHandles = new TreeLayerTaskHandle[splitChildCount];
                this.subTaskHandleCount = splitChildCount;
                for (int i = 0; i < splitChildCount; i++)
                    subTaskHandles[i] = new TreeLayerTaskHandle<V>(subTasks[i], execWorker, this, root, pool);

                execWorker.getQueue().addAll(Arrays.asList(subTaskHandles));
            } else {
                this.handleSubTaskException(new TaskExecutionException(new TaskCountExceededException("Task count exceeded")));
            }
        } else {//4: execute leaf task
            super.executeTask(execWorker);
        }
    }

    //***************************************************************************************************************//
    //                              4: task result                                                                   //                                                                                  //
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
                            parent.fillTaskResult(TASK_SUCCEED, parent.task.join(parent.subTaskHandles));//join children
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
