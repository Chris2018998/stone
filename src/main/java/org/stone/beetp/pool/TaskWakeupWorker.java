/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

/**
 * Pool task schedule worker
 *
 * @author Chris Liao
 * @version 1.0
 */

class TaskWakeupWorker extends Thread {

    private final PoolTaskCenter pool;

    public TaskWakeupWorker(PoolTaskCenter pool) {
        this.pool = pool;
    }

    public void run() {

    }
}
