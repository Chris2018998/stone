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

import java.util.concurrent.TimeUnit;

/**
 * Task handle
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskHandle {

    boolean isNew();

    boolean isRunning();

    boolean isCancelled();

    //task running completed,and a result object filled to this handle
    boolean isCompleted();

    //execute failed during task execution
    boolean isException();

    void callbackOnCompleted();

    boolean cancel(boolean mayInterruptIfRunning) throws BeeTaskException;

    Object get() throws BeeTaskException, InterruptedException;

    Object get(long timeout, TimeUnit unit) throws BeeTaskException, InterruptedException;

}
