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

    boolean isPeriodic();

    boolean isFixedDelay();

    long getPrevTime();

    long getNextTime();

    V getPrevResult() throws TaskException;
}
