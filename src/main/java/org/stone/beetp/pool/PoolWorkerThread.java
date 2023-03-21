/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.pool;

/**
 * Task Worker thread
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PoolWorkerThread extends Thread {
    private final TaskExecutionPool pool;
    private int state;

    public PoolWorkerThread(TaskExecutionPool pool, String name, boolean daemon) {
        this.pool = pool;
        super.setName(name);
        super.setDaemon(daemon);
    }

    public void run() {

    }
}