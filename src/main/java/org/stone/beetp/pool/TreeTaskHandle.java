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
import org.stone.beetp.BeeTreeTask;
import org.stone.beetp.pool.exception.TaskExecutionException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beetp.BeeTaskStates.TASK_CALL_EXCEPTION;
import static org.stone.beetp.BeeTaskStates.TASK_CALL_RESULT;

/**
 * join task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class TreeTaskHandle extends BaseHandle {
    //1: field of root
    private AtomicBoolean exceptionInd;
    //2: field of parent
    private TreeTaskHandle[] subTaskHandles;

    //3: fields of child task
    private int brotherSize;
    private BeeTreeTask task;
    private TreeTaskHandle root;
    private TreeTaskHandle parent;
    private AtomicInteger completedCount;//the complete count of sub tasks.

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    TreeTaskHandle(BeeTreeTask task, final BeeTaskCallback callback, TaskExecutionPool pool) {
        super(null, callback, true, pool);
        this.task = task;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    private TreeTaskHandle(BeeTreeTask task, TreeTaskHandle parent, int brotherSize, AtomicInteger completedCount, TaskExecutionPool pool, TreeTaskHandle root) {
        super(null, null, false, pool);
        this.task = task;
        this.root = root;
        this.parent = parent;
        this.brotherSize = brotherSize;
        this.completedCount = completedCount;
    }

    BeeTreeTask getTreeTask() {
        return task;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (subTaskHandles != null) {
            if (this.isRoot) {
                new Thread() {//async to cancel children
                    public void run() {
                        for (TreeTaskHandle childHandle : subTaskHandles)
                            childHandle.cancel(mayInterruptIfRunning);
                    }
                }.start();
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
    Object invokeTaskCall() throws Exception {
        return task.call(null);
    }

    void beforeExecute(TaskWorkThread thread) {
    }

    void afterExecute(TaskWorkThread thread) {
    }

    void executeTask(TaskWorkThread thread) {
        //2: try to split current task into sub tasks
        BeeTreeTask[] subTasks = this.task.getSubTasks();

        //3: push sub tasks to execute queue
        if (subTasks != null && subTasks.length > 0) {
            int subSize = subTasks.length;
            AtomicInteger completedCount = new AtomicInteger();
            TreeTaskHandle[] subJoinHandles = new TreeTaskHandle[subSize];
            TreeTaskHandle root = isRoot ? this : this.root;
            this.subTaskHandles = subJoinHandles;

            for (int i = 0; i < subSize; i++) {
                subJoinHandles[i] = new TreeTaskHandle(subTasks[i], this, subSize, completedCount, pool, root);
                pool.pushToExecutionQueue(subJoinHandles[i]);
            }
        } else {//4: execute leaf task
            super.executeTask();
        }
    }

    //***************************************************************************************************************//
    //                              4: task result                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void afterSetResult(final int state, final Object result) {
        if (brotherSize > 0) {
            if (state == TASK_CALL_EXCEPTION) {
                if (root.exceptionInd.compareAndSet(false, true)) {
                    root.setResult(state, result);
                    root.cancel(true);
                    workThread.incrCompletedCount();
                }
            } else {
                do {
                    int currentSize = completedCount.get();
                    if (currentSize == brotherSize) break;
                    if (completedCount.compareAndSet(currentSize, currentSize + 1)) {
                        if (currentSize + 1 == brotherSize) {
                            try {
                                parent.setResult(TASK_CALL_RESULT, parent.task.call(parent.subTaskHandles));//join children
                            } catch (Throwable e) {
                                if (root.exceptionInd.compareAndSet(false, true)) {
                                    root.setResult(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
                                    root.cancel(true);
                                }
                            }

                            if (parent.isRoot) {
                                pool.getTaskHoldingCount().decrementAndGet();
                                workThread.incrCompletedCount();
                            }
                        }

                        break;
                    }
                } while (true);
            }
        }
    }
}
