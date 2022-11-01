/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.synchronizer;

/**
 * Node state static definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ThreadNodeState {

    //Simple signal to exit waiting(used in class{#link ThreadWaitPool})
    public static final Object SIGNAL = new Object();

    //Thread waited timeout
    public static final Object TIMEOUT = new Object();

    //Thread interrupted during waiting
    public static final Object INTERRUPTED = new Object();
}
