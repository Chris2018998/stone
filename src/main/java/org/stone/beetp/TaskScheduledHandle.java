/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

import org.stone.beetp.pool.exception.TaskException;

/**
 * Task scheduled handle
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskScheduledHandle<T> extends TaskHandle<T> {

    boolean isPeriodic();

    boolean isFixedDelay();

    //nanoseconds(less than System.nanoTime())
    long getPrevTime();

    //value should be more than System.nanoTime(),when call done,then update time for next call
    long getNextTime();

    //retrieve result of prev call
    T getPrevResult() throws TaskException;
}
