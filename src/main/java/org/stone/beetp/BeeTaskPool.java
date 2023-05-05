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
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void init(BeeTaskServiceConfig config) throws BeeTaskPoolException, BeeTaskServiceConfigException;

    //***************************************************************************************************************//
    //                2: task submit(2)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskHandle submit(BeeTask task) throws BeeTaskException;

    BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException;

    //***************************************************************************************************************//
    //                3: task schedule(6)                                                                            //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException;

    BeeTaskHandle schedule(BeeTask task, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException;

    BeeTaskHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException;

    BeeTaskHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException;

    BeeTaskHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit) throws BeeTaskException;

    BeeTaskHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException;

    //***************************************************************************************************************//
    //                4: Pool terminate and clear(5)                                                                 //
    //***************************************************************************************************************//
    boolean isTerminated();

    boolean isTerminating();

    //return uncompleted tasks in queue
    List<BeeTask> terminate(boolean mayInterruptIfRunning) throws BeeTaskPoolException;

    //true:pool has been terminated
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    //clear all tasks in pool
    boolean clear(boolean mayInterruptIfRunning);

    //***************************************************************************************************************//
    //                5: Pool monitor(1)                                                                             //
    //***************************************************************************************************************//
    BeeTaskPoolMonitorVo getPoolMonitorVo();

}
