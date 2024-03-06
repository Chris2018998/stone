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
 * Static constant variables definition
 *
 * @author Chris Liao
 * @version 1.0
 */
class TaskPoolConstants {
    //pool state
    static final int POOL_NEW = 0;
    static final int POOL_STARTING = 1;
    static final int POOL_RUNNING = 2;
    static final int POOL_CLEARING = 3;
    static final int POOL_TERMINATING = 4;
    static final int POOL_TERMINATED = 5;

    //worker state
    static final Object WORKER_IDLE = new Object();
    static final Object WORKER_WORKING = new Object();
    static final Object WORKER_TERMINATED = new Object();
}


