/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import org.stone.beeop.BeeObjectPoolThreadFactory;

/**
 * Default implementation of work thread factory
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolThreadFactory implements BeeObjectPoolThreadFactory {

    /**
     * create a thread to scan idle-timeout objects and remove them from pool
     *
     * @param runnable a runnable to be executed by new thread instance
     * @return a created thread
     */
    public Thread createIdleScanThread(Runnable runnable) {
        Thread thead = new Thread(runnable);
        thead.setName("IdleScanThread");
        return thead;
    }

    /**
     * create a servant thread to search idle objects or create new objects,
     * and transfer hold objects to waiters in pool
     *
     * @param runnable a runnable to be executed by new thread instance
     * @return a created thread
     */
    public Thread createServantThread(Runnable runnable) {
        Thread thead = new Thread(runnable);
        thead.setName("ServantThread");
        return thead;
    }
}
