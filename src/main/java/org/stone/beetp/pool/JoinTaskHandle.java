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
final class JoinTaskHandle extends BaseHandle {
    //1: field of root
    private AtomicBoolean exceptionInd;
    private JoinTaskHandle[] subTaskHandles;

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
    private JoinTaskHandle(BeeTask task, JoinTaskHandle parent, int brotherSize, AtomicInteger completedCount, BeeTaskJoinOperator operator, TaskPoolImplement pool, JoinTaskHandle root) {
        super(task, null, false, pool);
        this.root = root;
        this.parent = parent;
        this.operator = operator;
        this.brotherSize = brotherSize;
        this.completedCount = completedCount;
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
                        for (JoinTaskHandle childHandle : subTaskHandles)
                            childHandle.cancel(mayInterruptIfRunning);
                    }
                }.start();
            } else {
                for (JoinTaskHandle childHandle : subTaskHandles)
                    childHandle.cancel(mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    //***************************************************************************************************************//
    //                                          4: execute task                                                      //
    //***************************************************************************************************************//
    void beforeExecuteTask() {
        if (this.isRoot) pool.getTaskRunningCount().incrementAndGet();
    }

    void execute() {
        //1: before execute
        beforeExecuteTask();

        //2: try to split current task into sub tasks
        BeeTask[] subTasks = operator.split(this.task);

        //3: push sub tasks to execute queue
        if (subTasks != null && subTasks.length > 0) {
            int subSize = subTasks.length;
            AtomicInteger completedCount = new AtomicInteger();
            JoinTaskHandle[] subJoinHandles = new JoinTaskHandle[subSize];
            JoinTaskHandle root = isRoot ? this : this.root;
            this.subTaskHandles = subJoinHandles;

            for (int i = 0; i < subSize; i++) {
                subJoinHandles[i] = new JoinTaskHandle(subTasks[i], this, subSize, completedCount, operator, pool, root);
                pool.pushToExecutionQueue(subJoinHandles[i]);
            }
        } else {//4: execute leaf task
            this.executeInternalTask();
        }
    }

    //***************************************************************************************************************//
    //                                  5: result method                                                             //
    //***************************************************************************************************************//
    void afterSetResult(final int state, final Object result) {
        if (brotherSize > 0) {
            if (state == TASK_CALL_EXCEPTION) {
                if (root.exceptionInd.compareAndSet(false, true)) {
                    pool.getTaskRunningCount().decrementAndGet();
                    pool.getTaskCompletedCount().incrementAndGet();

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
                                parent.setResult(TASK_CALL_RESULT, operator.join(parent.subTaskHandles));//join children
                            } catch (Throwable e) {
                                if (root.exceptionInd.compareAndSet(false, true)) {
                                    root.setResult(TASK_CALL_EXCEPTION, new TaskExecutionException(e));
                                    root.cancel(true);
                                }
                            }

                            if (parent.isRoot) {
                                pool.getTaskHoldingCount().decrementAndGet();
                                pool.getTaskRunningCount().decrementAndGet();
                                pool.getTaskCompletedCount().incrementAndGet();
                            }
                        }
                        break;
                    }
                } while (true);
            }
        }
    }
}
