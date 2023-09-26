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

/**
 * Bee Task Executor Interface,there are three implementation sub classes,every task can match one of them
 * <p>
 * OnceTaskExecutor
 * ScheduledTaskExecutor
 * JoinTaskExecutor
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskExecutor {

    //maybe put check logic here,for example: pool full check
    void beforeOffer(BeeTask task) throws Exception;

    //1:add or decr some atomic numbers
    void beforeExecute(BeeTask task) throws Exception;

    //important method(***)
    Object executeTask(BeeTask task) throws Exception;

    //1: set result back to handle(OnceTaskExecutor)
    //2: calculate next running time(ScheduledTaskExecutor)
    //3: set result to parent (JoinTaskExecutor)
    void afterExecute(BeeTask task, Object result) throws Exception;
}
