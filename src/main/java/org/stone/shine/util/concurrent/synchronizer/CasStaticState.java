/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

/**
 * Node state static definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class CasStaticState {

    //Simple signal to exit waiting(used in class{#link ThreadWaitingPool})
    public static final Object INIT = new Object();

    //Simple signal to exit waiting(used in class{#link ThreadWaitingPool})
    public static final Object SIGNAL = new Object();

    //Thread waited timeout
    public static final Object TIMEOUT = new Object();

    //Thread interrupted during waiting
    public static final Object INTERRUPTED = new Object();

    //node removed state
    public static final Object REMOVED = new Object();

}
