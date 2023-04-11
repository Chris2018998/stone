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
import org.stone.beetp.BeeTaskCallback;

/**
 * Task Handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class GenericTaskHandle extends AbstractTaskHandle {

    GenericTaskHandle(BeeTask task, int state, BeeTaskCallback callback, TaskExecutionPool pool) {
        super(task, state, callback, pool);
    }

    void beforeCall() {
    }

    void afterCallResult(Object result) {
    }

    void afterCallThrowing(Throwable e) {
    }

    void afterCallFinally(Throwable e) {
    }
}
