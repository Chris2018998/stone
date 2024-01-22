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

import org.stone.beetp.Task;
import org.stone.beetp.TaskCallback;
import org.stone.beetp.TaskJoinOperator;
import org.stone.beetp.pool.exception.TaskExecutionException;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beetp.TaskStates.TASK_EXEC_EXCEPTION;
import static org.stone.beetp.TaskStates.TASK_EXEC_RESULT;

/**
 * join task handle impl
 *
 * @author Chris Liao
 * @version 1.0
 */
final class JoinTaskHandle extends BaseHandle {
    //3: fields of child task
    JoinTaskHandle root;
    //1: field of root
    private AtomicBoolean exceptionInd;
    private JoinTaskHandle[] subTaskHandles;
    private JoinTaskHandle parent;
    private AtomicInteger countDown;//the complete count of sub tasks.
    private TaskJoinOperator operator;

    //***************************************************************************************************************//
    //                                          1: Constructor(2)                                                    //                                                                                  //
    //***************************************************************************************************************//
    //constructor for root task
    JoinTaskHandle(Task task, TaskJoinOperator operator, TaskCallback callback, TaskExecutionPool pool) {
        super(task, callback, pool);
        this.operator = operator;
        this.exceptionInd = new AtomicBoolean();
    }

    //constructor for children task
    private JoinTaskHandle(Task task, JoinTaskHandle parent, AtomicInteger countDown, TaskJoinOperator operator, TaskExecutionPool pool, JoinTaskHandle root) {
        super(task, pool);
        this.root = root;
        this.parent = parent;
        this.operator = operator;
        this.countDown = countDown;
    }

    //***************************************************************************************************************//
    //                                  3: task cancel(1)                                                            //
    //***************************************************************************************************************//
    public boolean cancel(final boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);

        if (subTaskHandles != null) {
            if (this.isRoot) {
                new AsynJoinCancelThread(root.subTaskHandles, mayInterruptIfRunning).start();
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
    void beforeExecute() {
    }

    void afterExecute(TaskWorkThread worker) {

    }

    void executeTask(TaskWorkThread worker) {
        //1: try to split current task into sub tasks
        Task[] subTasks = operator.split(this.task);

        //2: push sub tasks to execute queue
        if (subTasks != null && subTasks.length > 0) {
            int subSize = subTasks.length;
            JoinTaskHandle root = isRoot ? this : this.root;
            this.subTaskHandles = new JoinTaskHandle[subSize];//current task is parent
            AtomicInteger countDown = new AtomicInteger(subSize);
            Queue<BaseHandle> workQueue = worker.workQueue;

            for (int i = 0; i < subSize; i++) {
                subTaskHandles[i] = new JoinTaskHandle(subTasks[i], this, countDown, operator, pool, root);
                pool.pushToExecutionQueue(subTaskHandles[i], workQueue);
            }

            //pool.wakeupStealWorkers(subSize);
        } else {//4: execute leaf task
            super.executeTask(worker);
        }
    }

    //***************************************************************************************************************//
    //                                  5: result method                                                             //
    //***************************************************************************************************************//
    void afterSetResult(final int state, final Object result) {
        if (countDown == null) return;

        if (state == TASK_EXEC_EXCEPTION) {
            this.handleSubTaskException(result);
        } else {
            do {
                int currentSize = countDown.get();
                if (currentSize == 0) break;
                if (countDown.compareAndSet(currentSize, currentSize - 1)) {
                    if (currentSize == 1) {
                        try {
                            parent.setResult(TASK_EXEC_RESULT, operator.join(parent.subTaskHandles));//join children
                            if (parent.isRoot) {
                                pool.getTaskCount().decrementAndGet();
                                ((TaskWorkThread) Thread.currentThread()).completedCount++;
                            }
                        } catch (Throwable e) {
                            this.handleSubTaskException(new TaskExecutionException(e));
                        }
                    }
                    break;
                }
            } while (true);
        }
    }

    private void handleSubTaskException(Object result) {
        if (root.exceptionInd.compareAndSet(false, true)) {
            root.setResult(TASK_EXEC_EXCEPTION, result);
            pool.getTaskCount().decrementAndGet();
            ((TaskWorkThread) Thread.currentThread()).completedCount++;

            new AsynJoinCancelThread(root.subTaskHandles, true).start();
        }
    }

    private static class AsynJoinCancelThread extends Thread {
        private boolean mayInterruptIfRunning;
        private JoinTaskHandle[] subTaskHandles;

        AsynJoinCancelThread(JoinTaskHandle[] subTaskHandles, boolean mayInterruptIfRunning) {
            this.subTaskHandles = subTaskHandles;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
        }

        public void run() {
            for (JoinTaskHandle childHandle : subTaskHandles)
                childHandle.cancel(mayInterruptIfRunning);
        }
    }
}
