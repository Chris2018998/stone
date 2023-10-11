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

import org.stone.beetp.BeeTask;
import org.stone.beetp.BeeTaskCallback;
import org.stone.beetp.BeeTaskJoinOperator;
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
final class JoinTaskHandle extends PlainTaskHandle {
    //1: field of root
    private AtomicBoolean exceptionInd;
    //2: field of parent
    private List<JoinTaskHandle> childrenList;

    //3: fields of child task
    private int brotherSize;
    private JoinTaskHandle root;
    private JoinTaskHandle parent;
    private AtomicInteger completedCount;//the complete count of sub tasks.
    private BeeTaskJoinOperator operator;

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    JoinTaskHandle(BeeTask task, BeeTaskJoinOperator operator, final BeeTaskCallback callback, TaskPoolImplement pool) {
        super(task, callback, true, pool);
        this.operator = operator;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    JoinTaskHandle(BeeTask task, JoinTaskHandle parent, int brotherSize, AtomicInteger completedCount, BeeTaskJoinOperator operator, TaskPoolImplement pool, JoinTaskHandle root) {
        super(task, null, false, pool);
        this.root = root;
        this.parent = parent;
        this.operator = operator;
        this.brotherSize = brotherSize;
        this.completedCount = completedCount;
    }

    //***************************************************************************************************************//
    //                                  2: other(3)                                                                  //                                                                                  //
    //***************************************************************************************************************//
    JoinTaskHandle getRoot() {
        return root;
    }

    BeeTaskJoinOperator getJoinOperator() {
        return operator;
    }

    void setChildrenList(List<JoinTaskHandle> childrenList) {
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

    private void cancelChildrenTasks(List<JoinTaskHandle> childrenList, boolean mayInterruptIfRunning) {
        for (JoinTaskHandle childHandle : childrenList)
            childHandle.cancel(mayInterruptIfRunning);
    }

    //plugin method
    void afterSetResult(final int state, final Object result) {
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
    }
}
