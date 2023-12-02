/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beecp;

/**
 * A Thread factory,which is used to create some work threads in connection pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPoolThreadFactory {

    /**
     * create thread to scan idle-timeout connections and remove them from pool
     *
     * @param runnable a runnable to be executed by new thread instance
     * @return a created thread
     */
    Thread createIdleScanThread(Runnable runnable);

    /**
     * create a servant thread to search idle connections or create new connections,
     * and transfer hold connections to waiters in pool
     *
     * @param runnable a runnable to be executed by new thread instance
     * @return a created thread
     */
    Thread createServantThread(Runnable runnable);

}
