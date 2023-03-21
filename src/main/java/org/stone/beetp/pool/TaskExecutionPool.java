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

import org.stone.beetp.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Task Pool Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskExecutionPool implements BeeTaskPool {

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeTaskServiceConfig config) throws Exception {

    }

    //***************************************************************************************************************//
    //                2: task submit methods(2)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws Exception {
        return null;
        //@todo
    }

    public List<BeeTaskHandle> submit(List<BeeTask> taskList) throws Exception {
        return null;
        //@todo
    }

    //***************************************************************************************************************//
    //                3: Pool terminate and clear(5)                                                                 //                                                                                  //
    //***************************************************************************************************************//
    public boolean isTerminated() {
        return true;
        //@todo
    }

    public boolean isTerminating() {
        return true;
        //@todo
    }

    public List<BeeTask> terminate(boolean cancelRunningTask) throws BeeTaskPoolException {
        return null;
        //@todo
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
        //@todo
    }

    public void clear(boolean cancelRunningTask) throws BeeTaskPoolException {

    }

    //***************************************************************************************************************//
    //                4: Pool monitor(1)                                                                             //                                                                                  //
    //***************************************************************************************************************//
    public BeeTaskPoolMonitorVo getPoolMonitorVo() {
        return null;
        //@todo
    }
}
