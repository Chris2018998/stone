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
    //Waiting(for wakeup,success or retry?)
    public static final int WAITING = 1;

    //Thread wait timeout in queue(chain)
    public static final int WAITED_TIMEOUT = 2;

    //Thread interrupted during waiting
    public static final int WAITED_INTERRUPTED = 3;

    //Thread has acquired a permit or lock
    public static final int ACQUIRED_SUCCESS = 4;

    //Thread got a notify to re-acquire permit or lock
    public static final int ACQUIRE_RETRY = 5;

    //Node mark as empty (NODE has been removed,for example: remove head or tail)
    public static final int EMPTY = 6;
}
