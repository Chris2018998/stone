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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private AtomicInteger countDown;//the complete count of sub tasks.

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    TreeLayerTaskHandle(TreeLayerTask task, final TaskAspect callback, PoolTaskCenter pool) {
        super(null, callback, pool, true);
        this.task = task;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    private TreeLayerTaskHandle(TreeLayerTask task, TreeLayerTaskHandle parent, AtomicInteger countDown, PoolTaskCenter pool, TreeLayerTaskHandle root) {
        super(null, null, pool, false);
        this.task = task;
        this.root = root;
        this.parent = parent;
        this.countDown = countDown;
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
                for (TreeLayerTaskHandle childHandle : subTaskHandles)
                    childHandle.cancel(mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    //***************************************************************************************************************//
    //                                          4: execute task                                                      //
    //***************************************************************************************************************//
    protected void beforeExecute() {
    }

    private void afterExecute(TaskExecuteWorker worker) {
    }

    protected Object invokeTaskCall() throws Exception {
        return task.join(null);
    }

    protected void executeTask(TaskExecuteWorker worker) {
//        //2: try to split current task into sub tasks
//        TreeLayerTask[] subTasks = this.task.getSubTasks();
//
//        //3: push sub tasks to execute queue
//        if (subTasks != null && subTasks.length > 0) {
//            int subSize = subTasks.length;
//            AtomicInteger countDownLatch = new AtomicInteger(subSize);
//            TreeLayerTaskHandle[] subJoinHandles = new TreeLayerTaskHandle[subSize];
//            TreeLayerTaskHandle root = isRoot() ? this : this.root;
//            this.subTaskHandles = subJoinHandles;
//            Queue<PoolTaskHandle> workQueue = worker.workQueue;
//
//            for (int i = 0; i < subSize; i++) {
//                subJoinHandles[i] = new TreeLayerTaskHandle(subTasks[i], this, countDownLatch, pool, root);
//                pool.pushToExecutionQueue(subTaskHandles[i], workQueue);
//            }
//        } else {//4: execute leaf task
//            super.executeTask(worker);
//        }
    }

    //***************************************************************************************************************//
    //                              4: task result                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void afterSetResult(final int state, final Object result) {
//        if (countDown == null) return;
//
//        if (state == TASK_EXEC_EXCEPTION) {
//            this.handleTreeSubTaskException(result);
//        } else {
//            do {
//                int currentSize = countDown.get();
//                if (currentSize == 0) break;
//                if (countDown.compareAndSet(currentSize, currentSize - 1)) {
//                    if (currentSize == 1) {
//                        try {
//                            parent.setResult(TASK_EXEC_RESULT, parent.task.call(parent.subTaskHandles));//join children
//                            if (parent.isRoot) {
//                                pool.getTaskCount().decrementAndGet();
//                                ((TaskWorkThread) Thread.currentThread()).completedCount++;
//                            }
//                        } catch (Throwable e) {
//                            this.handleTreeSubTaskException(new TaskExecutionException(e));
//                        }
//                    }
//                    break;
//                }
//            } while (true);
//        }
    }

    private void handleTreeSubTaskException(Object result) {
//        if (root.exceptionInd.compareAndSet(false, true)) {
//            root.setResult(TASK_EXEC_EXCEPTION, result);
//            pool.getTaskCount().decrementAndGet();
//            ((TaskWorkThread) Thread.currentThread()).completedCount++;
//            new AsynTreeCancelThread(root.subTaskHandles, true).start();
//        }
    }

    private static class AsynTreeCancelThread extends Thread {
        private final boolean mayInterruptIfRunning;
        private final TreeLayerTaskHandle[] subTaskHandles;

        AsynTreeCancelThread(TreeLayerTaskHandle[] subTaskHandles, boolean mayInterruptIfRunning) {
            this.subTaskHandles = subTaskHandles;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
        }

        public void run() {
            for (TreeLayerTaskHandle childHandle : subTaskHandles)
                childHandle.cancel(mayInterruptIfRunning);
        }
    }
}
