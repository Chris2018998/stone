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
import org.stone.beetp.BeeTaskException;
import org.stone.beetp.BeeTaskHandle;

/**
 * Once Task Executor
 *
 * @author Chris Liao
 * @version 1.0
 */
public class JoinExecFactory extends TaskExecFactory {

    JoinExecFactory(TaskPoolImplement pool) {
        super(pool);
    }

    BaseHandle createHandle(TaskConfig config) throws BeeTaskException {
        BeeTask task = config.getTask();
        if (task == null) throw new BeeTaskException("Task can't be null");
        if (config.getJoinOperator() == null) throw new BeeTaskException("Join Splitter can't be null");
        return new JoinTaskHandle(task, null, 0, this);
    }

    public void beforeOffer(BeeTask task) throws BeeTaskException {

    }

    public void beforeExecute(BeeTaskHandle handle) throws BeeTaskException {

    }

    public Object executeTask(BeeTaskHandle handle) throws BeeTaskException {
        return null;
    }

    public void afterExecute(BeeTaskHandle handle, Object result) throws BeeTaskException {

    }
}