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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.stone.beetp.pool.PoolConstants.TASK_EXCEPTIONAL;
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

    private TreeLayerTaskHandle(TreeLayerTask<V> task, TreeLayerTaskHandle<V> parent, TreeLayerTaskHandle<V> root,
                                PoolTaskCenter pool, ConcurrentLinkedQueue<PoolTaskHandle<?>> taskBucket) {//for sub tasks
        super(null, null, pool, false);
        this.task = task;
        this.root = root;
        this.parent = parent;
        this.taskBucket = taskBucket;
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
    protected Object invokeTaskCall() throws Exception {
        return task.join(null);
    }

    protected void executeTask(TaskExecutionWorker worker) {
        TreeLayerTask<V>[] subTasks = this.task.getSubTasks();

        if (subTasks != null && subTasks.length > 0) {
            worker.incrementCompletedCount();
            int subTaskCount = subTasks.length;
            if (pool.incrementTaskCountForInternal(subTaskCount - 1)) {
                this.subTaskHandles = new TreeLayerTaskHandle[subTaskCount];
                this.subTaskHandleCount = subTaskCount;

                ConcurrentLinkedQueue<PoolTaskHandle<?>> bucket = worker.getTaskBucket();
                for (int i = 0; i < subTaskCount; i++)
                    subTaskHandles[i] = new TreeLayerTaskHandle<V>(subTasks[i], this, root, pool, bucket);

                bucket.addAll(Arrays.asList(subTaskHandles));
                pool.attemptActivateAllWorkers();
            } else {
                pool.decrementTaskCount();
                this.handleSubTaskException(new TaskExecutionException(new TaskCountExceededException("Task count exceeded")));
            }
        } else {//4: execute leaf task
            super.executeTask(worker);
        }
    }

    //***************************************************************************************************************//
    //                              4: task result                                                                   //                                                                                  //
    //***************************************************************************************************************//
    protected void afterExecute(boolean successful, Object result) {
        if (successful) {
            if (parent != null) parent.joinSubTasks();
        } else {
            this.handleSubTaskException(result);
        }
    }

    private void joinSubTasks() {
        do {
            int currentSize = subTaskHandleCount;
            if (unCompletedCountUpd.compareAndSet(this, currentSize, currentSize - 1)) {
                if (currentSize == 1) {
                    try {
                        fillTaskResult(TASK_SUCCEED, task.join(subTaskHandles));
                    } catch (Throwable e) {
                        this.handleSubTaskException(new TaskExecutionException(e));
                    }
                }
                break;
            }
        } while (true);
    }

    private void handleSubTaskException(Object result) {
        if (exceptionIndUpd.compareAndSet(root, 0, 1)) {
            root.fillTaskResult(TASK_EXCEPTIONAL, result);
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
