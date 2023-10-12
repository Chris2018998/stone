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
final class TreeTaskHandle extends BaseTaskHandle {
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
    TreeTaskHandle(BeeTreeTask task, final BeeTaskCallback callback, TaskPoolImplement pool) {
        super(true, callback, pool);
        this.task = task;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    TreeTaskHandle(BeeTreeTask task, TreeTaskHandle parent, int brotherSize, AtomicInteger completedCount, TaskPoolImplement pool, TreeTaskHandle root) {
        super(false, null, pool);
        this.task = task;
        this.root = root;
        this.parent = parent;
        this.brotherSize = brotherSize;
        this.completedCount = completedCount;
    }

    //***************************************************************************************************************//
    //                                  2: other(3)                                                                  //                                                                                  //
    //***************************************************************************************************************//
    BeeTreeTask getTask() {
        return task;
    }

    TreeTaskHandle getRoot() {
        return root;
    }

    void setSubTaskHandles(TreeTaskHandle[] subTaskHandles) {
        this.subTaskHandles = subTaskHandles;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (subTaskHandles != null) {
            if (this.isRoot()) {
                new Thread() {//async to cancel children
                    public void run() {
                        cancelChildrenTasks(subTaskHandles, mayInterruptIfRunning);
                    }
                }.start();
            } else {
                cancelChildrenTasks(subTaskHandles, mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    private void cancelChildrenTasks(TreeTaskHandle[] subTaskHandles, boolean mayInterruptIfRunning) {
        for (TreeTaskHandle childHandle : subTaskHandles)
            childHandle.cancel(mayInterruptIfRunning);
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
                }
            } else {
                do {
                    int currentSize = completedCount.get();
                    if (currentSize == brotherSize) break;
                    if (completedCount.compareAndSet(currentSize, currentSize + 1)) {
                        if (currentSize + 1 == brotherSize) {
                            try {
                                parent.setResult(TASK_CALL_RESULT, parent.getTask().call(parent.subTaskHandles));//join children
                            } catch (Throwable e) {
                                if (root.exceptionInd.compareAndSet(false, true)) {
                                    root.setResult(state, new TaskExecutionException(e));
                                    root.cancel(true);
                                }
                            }

                            if (parent.isRoot()) {
                                getPool().getTaskRunningCount().decrementAndGet();
                                getPool().getTaskCompletedCount().incrementAndGet();
                            }
                        }

                        break;
                    }
                } while (true);
            }
        }
    }
}
