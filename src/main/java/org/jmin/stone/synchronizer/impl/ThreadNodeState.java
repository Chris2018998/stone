/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

/**
 * Node state static definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ThreadNodeState {

    //Running
    public static final Object RUNNING = new Object();

    //Waiting
    public static final Object WAITING = new Object();

    //Simple signal to exit waiting(used in class{#link ThreadWaitPool1})
    public static final Object NOTIFIED = new Object();

    //Signal to re-acquire a permit or a access lock
    public static final Object ACQUIRE = new Object();

    //Has acquired a permit or a access lock
    public static final Object ACQUIRED = new Object();

    //Thread waited timeout
    public static final Object TIMEOUT = new Object();

    //Thread interrupted during waiting
    public static final Object INTERRUPTED = new Object();
}
