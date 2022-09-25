/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

/**
 * @author Chris Liao
 * @version 1.0
 */
final class ThreadNodeState {

    //Waiting
    public static final int WAITING = 1;

    //Simple signal to exit waiting(used in class{#link ThreadWaitPool})
    public static final int NOTIFIED = 2;

    //Has acquired a permit or a access lock
    public static final int ACQUIRED = 3;

    //Signal to re-acquire a permit or access lock
    public static final int RE_ACQUIRE = 4;

    //Thread waited timeout
    public static final int TIMEOUT = 5;

    //Thread interrupted during waiting
    public static final int INTERRUPTED = 6;

    //Node mark as empty (NODE has been removed,for example: remove head or tail)
    public static final int EMPTY = 7;
}
