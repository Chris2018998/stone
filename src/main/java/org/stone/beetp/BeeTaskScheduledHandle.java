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

/**
 * Task scheduled handle
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskScheduledHandle<T> extends BeeTaskHandle<T> {

    boolean isPeriodic();

    boolean isFixedDelay();

    //nanoseconds(less than System.nanoTime())
    long getPrevCallTime();

    //value should be more than System.nanoTime(),when call done,then update time for next call
    long getNextCallTime();

    //retrieve result of prev call
    T getPrevCallResult() throws BeeTaskException;
}
