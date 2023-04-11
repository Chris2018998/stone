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
    void init(BeeTaskServiceConfig config) throws BeeTaskPoolException;

    //***************************************************************************************************************//
    //                2: generic task(2)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskHandle submit(BeeTask task) throws BeeTaskException, BeeTaskPoolException;

    BeeTaskHandle submit(BeeTask task, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException;

    //***************************************************************************************************************//
    //                3: schedule task(6)                                                                            //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskHandle schedule(BeeTask task, long delay, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException;

    BeeTaskHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException;

    BeeTaskHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long period, TimeUnit unit) throws BeeTaskException, BeeTaskPoolException;

    BeeTaskHandle schedule(BeeTask task, long delay, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException;

    BeeTaskHandle scheduleAtFixedRate(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException;

    BeeTaskHandle scheduleWithFixedDelay(BeeTask task, long initialDelay, long period, TimeUnit unit, BeeTaskCallback callback) throws BeeTaskException, BeeTaskPoolException;

    //***************************************************************************************************************//
    //                4: Pool terminate and clear(5)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    boolean isTerminated();

    boolean isTerminating();

    List<BeeTask> terminate(boolean mayInterruptIfRunning) throws BeeTaskPoolException;

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    boolean clear(boolean mayInterruptIfRunning);

    //***************************************************************************************************************//
    //                5: Pool monitor(1)                                                                             //
    //***************************************************************************************************************//
    BeeTaskPoolMonitorVo getPoolMonitorVo();

}
