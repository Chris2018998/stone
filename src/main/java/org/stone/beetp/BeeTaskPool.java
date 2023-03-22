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
    void init(BeeTaskServiceConfig config) throws Exception;

    //***************************************************************************************************************//
    //                2: task submit methods(1)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskHandle submit(BeeTask task) throws BeeTaskException;

    //***************************************************************************************************************//
    //                3: Pool terminate and clear(5)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    boolean isTerminated();

    boolean isTerminating();

    List<BeeTask> terminate(boolean cancelRunningTask) throws BeeTaskPoolException;

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    void clear(boolean cancelRunningTask) throws BeeTaskPoolException;

    //***************************************************************************************************************//
    //                4: Pool monitor(1)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    BeeTaskPoolMonitorVo getPoolMonitorVo();

}
