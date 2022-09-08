/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.chain;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class SynchronizeNodeState {

    /**
     * waitStatus value to indicate thread has timeout(remove from chain)
     */
    public static final int TIMEOUT = 0;

    /**
     * waitStatus value to indicate thread has interrupted(remove from chain)
     */
    public static final int INTERRUPTED = 1;


}
