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
import org.stone.beetp.BeeTaskJoinOperator;
import org.stone.beetp.BeeTreeTask;
import org.stone.beetp.pool.exception.TaskExecutionException;

import java.util.List;
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
    private List<TreeTaskHandle> childrenList;

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
    TreeTaskHandle(BeeTreeTask task, BeeTaskJoinOperator operator, final BeeTaskCallback callback, TaskPoolImplement pool) {
        super(true, callback, pool);
        this.task = task;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    TreeTaskHandle(BeeTreeTask task, TreeTaskHandle parent, int brotherSize, AtomicInteger completedCount, BeeTaskJoinOperator operator, TaskPoolImplement pool, TreeTaskHandle root) {
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
    TreeTaskHandle getRoot() {
        return root;
    }

    void setChildrenList(List<TreeTaskHandle> childrenList) {
        this.childrenList = childrenList;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (childrenList != null) {
            if (this.isRoot()) {
                new Thread() {//async to cancel children
                    public void run() {
                        cancelChildrenTasks(childrenList, mayInterruptIfRunning);
                    }
                }.start();
            } else {
                cancelChildrenTasks(childrenList, mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    private void cancelChildrenTasks(List<TreeTaskHandle> childrenList, boolean mayInterruptIfRunning) {
        for (TreeTaskHandle childHandle : childrenList)
            childHandle.cancel(mayInterruptIfRunning);
    }

    //***************************************************************************************************************//
    //                              4: join task result                                                              //                                                                                  //
    //***************************************************************************************************************//
    void setResult(int state, Object result) {
        //1: set result and state
        this.result = result;
        this.state = state;
        this.workThread = null;

        //2: incr completed count
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
                                parent.setResult(TASK_CALL_RESULT, operator.join(parent.childrenList));//join children
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
                            break;
                        }
                    }
                } while (true);
            }
        }

        //3: wakeup waiters on root task
        if (getWaitQueue() != null) this.wakeupWaitersInGetting();

        //4: execute callback for root task
        if (this.isRoot() && this.getCallback() != null) {
            try {
                this.getCallback().afterCall(state, result, this);
            } catch (final Throwable e) {
                //do nothing
            }
        }
    }
}