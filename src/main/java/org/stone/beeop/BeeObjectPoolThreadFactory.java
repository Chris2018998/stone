/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

/**
 * A Thread factory,which is used to create some work threads in object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectPoolThreadFactory {

    /**
     * create a thread to scan idle-timeout objects and remove them from pool
     *
     * @param runnable a runnable to be executed by new thread instance
     * @return a created thread
     */
    Thread createIdleScanThread(Runnable runnable);

    /**
     * create a servant thread to search idle objects or create new objects,
     * and transfer hold objects to waiters in pool
     *
     * @param runnable a runnable to be executed by new thread instance
     * @return a created thread
     */
    Thread createServantThread(Runnable runnable);

}
