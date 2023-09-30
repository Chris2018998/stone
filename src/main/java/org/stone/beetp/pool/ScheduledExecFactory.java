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

    ScheduledExecFactory(TaskPoolImplement pool) {
        super(null);
    }

    public BaseHandle createHandle(TaskConfig config) throws BeeTaskException {
        BeeTask task = config.getTask();
        if (task == null) throw new BeeTaskException("Task can't be null");
        if (config.getTimeUnit() == null) throw new BeeTaskException("Time unit can't be null");
        int scheduleType = config.getScheduleType();
        if (config.getInitialDelay() < 0)
            throw new BeeTaskException(scheduleType == 1 ? "Delay" : "Initial delay" + " time can't be less than zero");
        if (config.getIntervalDelay() <= 0 && scheduleType != 1)
            throw new BeeTaskException(scheduleType == 2 ? "Period" : "Delay" + " time must be greater than zero");

        return new OnceTaskHandle(config.getTask(), config.getCallback(), null, this);
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