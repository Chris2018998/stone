/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.execution;

import org.stone.beetp.TaskCallback;
import org.stone.beetp.TreeTask;
import org.stone.beetp.exception.TaskExecutionException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beetp.TaskStates.TASK_CALL_EXCEPTION;
import static org.stone.beetp.TaskStates.TASK_CALL_RESULT;

/**
 * join task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class TreeTaskHandle extends BaseHandle {
    TreeTaskHandle root;
    //1: field of root
    private AtomicBoolean exceptionInd;
    //2: field of parent
    private TreeTaskHandle[] subTaskHandles;
    //3: fields of child task
    private TreeTask task;
    private TreeTaskHandle parent;
    private AtomicInteger countDown;//the complete count of sub tasks.

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    TreeTaskHandle(TreeTask task, final TaskCallback callback, TaskExecutionPool pool) {
        super(null, callback, pool);
        this.task = task;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    private TreeTaskHandle(TreeTask task, TreeTaskHandle parent, AtomicInteger countDown, TaskExecutionPool pool, TreeTaskHandle root) {
        super(null, null, pool);
        this.task = task;
        this.root = root;
        this.parent = parent;
        this.countDown = countDown;
    }

    TreeTask getTreeTask() {
        return task;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (subTaskHandles != null) {
            if (this.isRoot) {
                new AsynTreeCancelThread(subTaskHandles, mayInterruptIfRunning).start();
            } else {
                for (TreeTaskHandle childHandle : subTaskHandles)
                    childHandle.cancel(mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    //***************************************************************************************************************//
    //                                          4: execute task                                                      //
    //***************************************************************************************************************//
    void beforeExecute() {
    }

    void afterExecute(TaskWorkThread worker) {
    }

    Object invokeTaskCall() throws Exception {
        return task.call(null);
    }

    void executeTask(TaskWorkThread worker) {
        //2: try to split current task into sub tasks
        TreeTask[] subTasks = this.task.getSubTasks();

        //3: push sub tasks to execute queue
        if (subTasks != null && subTasks.length > 0) {
            int subSize = subTasks.length;
            AtomicInteger countDownLatch = new AtomicInteger(subSize);
            TreeTaskHandle[] subJoinHandles = new TreeTaskHandle[subSize];
            TreeTaskHandle root = isRoot ? this : this.root;
            this.subTaskHandles = subJoinHandles;
            ConcurrentLinkedQueue<BaseHandle> workQueue = worker.workQueue;

            for (int i = 0; i < subSize; i++) {
                subJoinHandles[i] = new TreeTaskHandle(subTasks[i], this, countDownLatch, pool, root);
                workQueue.offer(subJoinHandles[i]);
            }
        } else {//4: execute leaf task
            super.executeTask(worker);
        }
    }

    //***************************************************************************************************************//
    //                              4: task result                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void afterSetResult(final int state, final Object result) {
        if (countDown == null) return;

        if (state == TASK_CALL_EXCEPTION) {
            this.handleTreeSubTaskException(result);
        } else {
            do {
                int currentSize = countDown.get();
                if (currentSize == 0) break;
                if (countDown.compareAndSet(currentSize, currentSize - 1)) {
                    if (currentSize == 1) {
                        try {
                            parent.setResult(TASK_CALL_RESULT, parent.task.call(parent.subTaskHandles));//join children
                            if (parent.isRoot) {
                                pool.getTaskCount().decrementAndGet();
                                ((TaskWorkThread) Thread.currentThread()).completedCount++;
                            }
                        } catch (Throwable e) {
                            this.handleTreeSubTaskException(new TaskExecutionException(e));
                        }
                    }
                    break;
                }
            } while (true);
        }
    }

    private void handleTreeSubTaskException(Object result) {
        if (root.exceptionInd.compareAndSet(false, true)) {
            root.setResult(TASK_CALL_EXCEPTION, result);
            pool.getTaskCount().decrementAndGet();
            ((TaskWorkThread) Thread.currentThread()).completedCount++;
            new AsynTreeCancelThread(root.subTaskHandles, true).start();
        }
    }

    private static class AsynTreeCancelThread extends Thread {
        private boolean mayInterruptIfRunning;
        private TreeTaskHandle[] subTaskHandles;

        AsynTreeCancelThread(TreeTaskHandle[] subTaskHandles, boolean mayInterruptIfRunning) {
            this.subTaskHandles = subTaskHandles;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
        }

        public void run() {
            for (TreeTaskHandle childHandle : subTaskHandles)
                childHandle.cancel(mayInterruptIfRunning);
        }
    }
}
