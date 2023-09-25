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

/**
 * Bee Task Executor Interface,exists three implementation classes for it
 * <p>
 * OnceTaskExecutor
 * ScheduledTaskExecutor
 * JoinTaskExecutor
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskExecutor {

    void beforeOffer(BeeTask task) throws Exception;

    void beforeExecuteTask(BeeTask task) throws Exception;

    void executeTask(BeeTask task) throws Exception;

    void afterExecuteTask(BeeTask task, Object result) throws Exception;

}
