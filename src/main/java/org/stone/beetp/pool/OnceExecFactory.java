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

import org.stone.beetp.BeeTaskException;
import org.stone.beetp.BeeTaskHandle;

/**
 * Once Task Executor
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class OnceExecFactory extends TaskExecFactory {

    OnceExecFactory(TaskPoolImplement pool) {
        super(null);
    }

    public void beforeExecute(BeeTaskHandle handle) throws BeeTaskException {

    }

    public Object executeTask(BeeTaskHandle handle) throws BeeTaskException {
        return null;
    }

    public void afterExecute(BeeTaskHandle handle, Object result) throws BeeTaskException {

    }
}