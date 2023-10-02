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
    private List<JoinTaskHandle> childrenList;
    private BeeTaskJoinOperator operator;

    //fields for child task
    private JoinTaskHandle parent;
    private AtomicInteger completedCount;//the complete count of sub tasks.

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    JoinTaskHandle(BeeTask task, BeeTaskJoinOperator operator, TaskPoolImplement pool) {
        super(task, null, pool);
        this.operator = operator;
    }

    //constructor for child task
    JoinTaskHandle(BeeTask task, JoinTaskHandle parent, AtomicInteger completedCount, BeeTaskJoinOperator operator, TaskPoolImplement pool) {
        super(task, null, pool);
        this.parent = parent;
        this.operator = operator;
        this.completedCount = completedCount;
    }

    //***************************************************************************************************************//
    //                              2: other(3)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    public JoinTaskHandle getParent() {
        return parent;
    }

    public BeeTaskJoinOperator getOperator() {
        return operator;
    }

    public void setChildrenList(List<JoinTaskHandle> childrenList) {
        this.childrenList = childrenList;
        this.childrenSize = childrenList != null ? childrenList.size() : 0;
    }

    //***************************************************************************************************************//
    //                              3: add completed count of children task                                                          //                                                                                  //
    //***************************************************************************************************************//
    public void incrementSubCompleted() {
        do {
            int currentSize = completedCount.incrementAndGet();
            if (currentSize == childrenSize) return;
            if (completedCount.compareAndSet(childrenSize, childrenSize + 1)) {
                if (currentSize == childrenSize)
                    this.setDone(TASK_CALL_RESULT, operator.join(childrenList));
            }
        } while (true);
    }
}
