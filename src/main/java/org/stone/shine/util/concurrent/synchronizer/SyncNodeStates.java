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
public final class SyncNodeStates {

    //RUNNING
    public static final Object RUNNING = new Object();

    //timeout
    public static final Object TIMEOUT = new Object();

    //interruption
    public static final Object INTERRUPTED = new Object();

    //node has removed from chain
    public static final Object REMOVED = new Object();

}
