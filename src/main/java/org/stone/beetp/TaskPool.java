/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import org.stone.beetp.exception.TaskException;
import org.stone.beetp.exception.TaskPoolException;
import org.stone.beetp.exception.TaskServiceConfigException;

import java.util.concurrent.TimeUnit;

/**
 * Task Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskPool {

    //***************************************************************************************************************//
    //                1: execution initialize method(1)                                                                   //
    //***************************************************************************************************************//
    void init(TaskServiceConfig config) throws TaskPoolException, TaskServiceConfigException;

    //***************************************************************************************************************//
    //                2: task submit(6)                                                                              //
    //***************************************************************************************************************//
    TaskHandle submit(Task task) throws TaskException;

    TaskHandle submit(Task task, TaskCallback callback) throws TaskException;

    TaskHandle submit(Task task, TaskJoinOperator joinOperator) throws TaskException;

    TaskHandle submit(Task task, TaskJoinOperator joinOperator, TaskCallback callback) throws TaskException;

    TaskHandle submit(TreeTask task) throws TaskException;

    TaskHandle submit(TreeTask task, TaskCallback callback) throws TaskException;

    //***************************************************************************************************************//
    //                3: scheduled task(6)                                                                           //
    //***************************************************************************************************************//
    TaskScheduledHandle schedule(Task task, long delay, TimeUnit unit) throws TaskException;

    TaskScheduledHandle schedule(Task task, long delay, TimeUnit unit, TaskCallback callback) throws TaskException;

    TaskScheduledHandle scheduleAtFixedRate(Task task, long initialDelay, long period, TimeUnit unit) throws TaskException;

    TaskScheduledHandle scheduleAtFixedRate(Task task, long initialDelay, long period, TimeUnit unit, TaskCallback callback) throws TaskException;

    TaskScheduledHandle scheduleWithFixedDelay(Task task, long initialDelay, long delay, TimeUnit unit) throws TaskException;

    TaskScheduledHandle scheduleWithFixedDelay(Task task, long initialDelay, long delay, TimeUnit unit, TaskCallback callback) throws TaskException;

    //***************************************************************************************************************//
    //                4: Pool clear(2)                                                                               //
    //***************************************************************************************************************//
    //clear all tasks in execution
    boolean clear(boolean mayInterruptIfRunning);

    //clear all tasks in execution
    boolean clear(boolean mayInterruptIfRunning, TaskServiceConfig config) throws TaskServiceConfigException;

    //***************************************************************************************************************//
    //                5: Pool termination(4)                                                                         //
    //***************************************************************************************************************//
    boolean isTerminated();

    boolean isTerminating();

    //return uncompleted tasks in queue
    CancelledTasks terminate(boolean mayInterruptIfRunning) throws TaskPoolException;

    //true:execution has been terminated
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    //***************************************************************************************************************//
    //                6: Pool monitor(1)                                                                             //
    //***************************************************************************************************************//
    TaskPoolMonitorVo getPoolMonitorVo();

}
