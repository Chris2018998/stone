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
public class ScheduledExecFactory extends TaskExecFactory {

    public ScheduledExecFactory(TaskPoolImplement pool) {
        super(null);
    }

    public BeeTaskHandle createHandle(TaskConfig config) throws Exception {
        return new OnceTaskHandle(config.getTask(), config.getCallback(), null);
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