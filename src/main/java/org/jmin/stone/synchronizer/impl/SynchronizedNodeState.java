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
final class SynchronizedNodeState {

    //Thread wait for a released lock,if thread got it,then set as a exclusive lock or permit
    public static final int WAIT_EXCLUSIVE = 1;

    //Thread wait for a released lock,if thread got it,then set as a shared lock
    public static final int WAIT_SHARED = 2;

    //Thread wait timeout in queue(chain)
    public static final int WAIT_TIMEOUT = 3;

    //Thread interrupted during waiting
    public static final int WAIT_INTERRUPTED = 4;

    //Thread has acquired a permit or lock
    public static final int ACQUIRE_SUCCESS = 5;

    //Thread got a notify to re-acquire permit or lock
    public static final int ACQUIRE_RETRY = 6;

    //Node mark as empty (NODE has been removed,for example: remove head or tail)
    public static final int EMPTY = 7;
}
