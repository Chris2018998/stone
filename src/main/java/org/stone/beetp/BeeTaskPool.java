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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Task Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskPool {

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //
    //***************************************************************************************************************//
    void init(BeeTaskServiceConfig config) throws BeeTaskPoolException, BeeTaskServiceConfigException;

    //***************************************************************************************************************//
    //                2: task submit(2)                                                                              //
    //***************************************************************************************************************//
    BeeTaskHandle submit(BeeTask task) throws BeeTaskException;

    BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException;

    //***************************************************************************************************************//
    //                3: task schedule(6)                                                                            //
    //***************************************************************************************************************//
    BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException;

    BeeTaskScheduledHandle schedule(BeeTask task, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException;

    BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException;

    BeeTaskScheduledHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException;

    BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit) throws BeeTaskException;

    BeeTaskScheduledHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException;

    //***************************************************************************************************************//
    //                4: Pool clear(2)                                                                               //
    //***************************************************************************************************************//
    //clear all tasks in pool
    boolean clear(boolean mayInterruptIfRunning);

    //clear all tasks in pool
    boolean clear(boolean mayInterruptIfRunning, BeeTaskServiceConfig config) throws BeeTaskServiceConfigException;

    //***************************************************************************************************************//
    //                5: Pool termination(4)                                                                         //
    //***************************************************************************************************************//
    boolean isTerminated();

    boolean isTerminating();

    //return uncompleted tasks in queue
    List<BeeTask> terminate(boolean mayInterruptIfRunning) throws BeeTaskPoolException;

    //true:pool has been terminated
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    //***************************************************************************************************************//
    //                6: Pool monitor(1)                                                                             //
    //***************************************************************************************************************//
    BeeTaskPoolMonitorVo getPoolMonitorVo();

}
