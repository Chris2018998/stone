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
 * handle of a timed task scheduled in pool.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskScheduledHandle<V> extends TaskHandle<V> {

    /**
     * Query task execution is whether periodic
     *
     * @return true that time periodic
     */
    boolean isPeriodic();

    /**
     * Query time delay type
     *
     * @return true that is fixed delay
     */
    boolean isFixedDelay();

    /**
     * Get last execution time of task
     *
     * @return milli seconds
     */
    long getLastTime();

    /**
     * Get task result of last execution
     *
     * @return execution result
     * @throws TaskException if failure of last execution
     */
    V getLastResult() throws TaskException;

    /**
     * Get next execution time of task
     *
     * @return milli seconds
     */
    long getNextTime();
}
