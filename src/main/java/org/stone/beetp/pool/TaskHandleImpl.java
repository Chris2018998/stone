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

import java.util.concurrent.TimeUnit;

/**
 * Task Handle Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskHandleImpl implements BeeTaskHandle {

    //***************************************************************************************************************//
    //                1: task state methods(5)                                                                       //                                                                                  //
    //***************************************************************************************************************//
    public boolean isNew() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    //task running completed,and a result object filled to this handle
    public boolean isCompleted() {
        return false;
    }

    //execute failed during task execution
    public boolean isExceptional() {
        return false;
    }

    //***************************************************************************************************************//
    //                2: task result get and cancel methods(3)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public Object get() throws BeeTaskException, InterruptedException {
        return null;
    }

    public Object get(long timeout, TimeUnit unit) throws BeeTaskException, InterruptedException {
        return null;
    }

    public boolean cancel(boolean mayInterruptIfRunning) throws BeeTaskException {
        return true;
    }
}
