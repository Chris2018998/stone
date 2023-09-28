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

import org.stone.beetp.BeeTaskHandle;

/**
 * Task Handle
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class BaseHandle implements BeeTaskHandle {
    private TaskExecFactory factory;

    public BaseHandle(TaskExecFactory factory) {
        this.factory = factory;
    }

    public TaskExecFactory getFactory() {
        return factory;
    }
}
