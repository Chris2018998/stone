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

import static org.stone.beetp.pool.TaskPoolConstants.TASK_CALL_RESULT;

/**
 * join task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinTaskHandle extends BaseHandle {
    //1: fields of a parent task
    private int childrenSize;
    private BeeTaskJoinOperator operator;
    private List<JoinTaskHandle> childrenList;

    //fields for child task
    private JoinTaskHandle parent;
    private AtomicInteger completedCount;//the complete count of sub tasks.

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    JoinTaskHandle(BeeTask task, BeeTaskJoinOperator operator, TaskPoolImplement pool) {
        super(task, null, pool, true);
        this.operator = operator;

    }

    //constructor for children task
    JoinTaskHandle(BeeTask task, JoinTaskHandle parent, AtomicInteger completedCount, BeeTaskJoinOperator operator, TaskPoolImplement pool) {
        super(task, null, pool, false);
        this.parent = parent;
        this.operator = operator;
        this.completedCount = completedCount;
    }

    //***************************************************************************************************************//
    //                              2: other(3)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskJoinOperator getJoinOperator() {
        return operator;
    }

    void setChildrenList(List<JoinTaskHandle> childrenList) {
        this.childrenList = childrenList;
        this.childrenSize = childrenList != null ? childrenList.size() : 0;
    }

    //***************************************************************************************************************//
    //                              3: join task result                                                              //                                                                                  //
    //***************************************************************************************************************//
    void setDone(int state, Object result) {
        //1: set result and state
        this.result = result;
        this.state.set(state);

        //2: incr completed count
        if (completedCount != null) {
            do {
                int currentSize = completedCount.get();
                if (currentSize == childrenSize) break;
                if (completedCount.compareAndSet(childrenSize, childrenSize + 1)) {
                    if (currentSize == childrenSize) {
                        parent.setDone(TASK_CALL_RESULT, operator.join(childrenList));//join children
                        pool.getTaskRunningCount().decrementAndGet();
                        pool.getTaskCompletedCount().incrementAndGet();
                    }
                    break;
                }
            } while (true);
        }

        //3: wakeup waiters on root task
        if (waitQueue != null) this.wakeupWaitersInGetting();
    }
}
