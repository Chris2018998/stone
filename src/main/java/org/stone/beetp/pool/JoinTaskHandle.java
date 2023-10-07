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
import org.stone.beetp.BeeTaskJoinOperator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beetp.BeeTaskStates.TASK_CALL_RESULT;

/**
 * join task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinTaskHandle extends BaseHandle {
    //1: fields of a parent task
    private List<JoinTaskHandle> childrenList;

    //fields for child task
    private JoinTaskHandle parent;
    private int splitSizeByParent;
    private AtomicInteger completedCount;//the complete count of sub tasks.
    private BeeTaskJoinOperator operator;

    // split
    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    JoinTaskHandle(BeeTask task, BeeTaskJoinOperator operator, TaskPoolImplement pool) {
        super(task, null, pool, true);
        this.operator = operator;
    }

    //constructor for children task
    JoinTaskHandle(BeeTask task, JoinTaskHandle parent, int splitSizeFromParent, AtomicInteger completedCount, BeeTaskJoinOperator operator, TaskPoolImplement pool) {
        super(task, null, pool, false);
        this.parent = parent;
        this.operator = operator;
        this.splitSizeByParent = splitSizeFromParent;
        this.completedCount = completedCount;
    }

    //***************************************************************************************************************//
    //                                  2: other(3)                                                                  //                                                                                  //
    //***************************************************************************************************************//
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

    //***************************************************************************************************************//
    //                              4: join task result                                                              //                                                                                  //
    //***************************************************************************************************************//
    void setResult(int state, Object result) {
        //1: set result and state
        this.result = result;
        this.state = state;
        this.workThread = null;

        //2: incr completed count
        if (splitSizeByParent > 0) {
            do {
                int currentSize = completedCount.get();
                if (currentSize == splitSizeByParent) break;
                if (completedCount.compareAndSet(currentSize, currentSize + 1)) {
                    if (currentSize + 1 == splitSizeByParent) {
                        parent.setResult(TASK_CALL_RESULT, operator.join(parent.childrenList));//join children
                        if (parent.isRoot()) {
                            pool.getTaskRunningCount().decrementAndGet();
                            pool.getTaskCompletedCount().incrementAndGet();
                        }
                    }
                    break;
                }
            } while (true);
        }

        //3: wakeup waiters on root task
        if (waitQueue != null) this.wakeupWaitersInGetting();
    }
}
