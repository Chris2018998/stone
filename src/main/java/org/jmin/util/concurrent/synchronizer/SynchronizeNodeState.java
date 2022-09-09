/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronizeNodeState {

    //wait in exclusive mode
    public static final int WAIT_FOR_EXCLUSIVE = 1;

    //wait in share mode
    public static final int WAIT_FOR_SHARE = 2;

    //permit acquired
    public static final int ACQUIRED = 3;

    //retry to acquire permit
    public static final int ACQUIRE_TRY = 4;

    //node timeout
    public static final int TIMEOUT = 5;

    //node thread interrupted
    public static final int INTERRUPTED = 6;

}
