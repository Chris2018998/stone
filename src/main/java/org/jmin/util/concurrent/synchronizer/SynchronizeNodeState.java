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
public final class SynchronizeNodeState {

    //wait for exclusive permit
    public static final int WAIT_FOR_EXCLUSIVE = 1;

    //wait for shared permit
    public static final int WAIT_FOR_SHARE = 2;

    //acquired permit
    public static final int ACQUIRED = 3;

    //try acquire permit(compete acquire mode)
    public static final int RETRY_ACQUIRE = 4;

    //node timeout
    public static final int TIMEOUT = 5;

    //node thread interrupted
    public static final int INTERRUPTED = 6;

    //node mark as empty (maybe removed head,removed tail)
    public static final int EMPTY = 7;
}
