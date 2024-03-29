/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent.synchronizer.chain;

/**
 * Node state static definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class SyncNodeStates {

    //RUNNING
    public static final Object RUNNING = new Object();

    //cancelled,abandon to get execution permit(timeout or interrupted)
    public static final Object CANCELLED = new Object();

    //REMOVED(pending to be removed from queue or have been removed)
    public static final Object REMOVED = new Object();

}
