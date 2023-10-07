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
 * pool util
 *
 * @author Chris Liao
 * @version 1.0
 */
class TaskPoolConstants {
    //pool state
    static final int POOL_NEW = 0;
    static final int POOL_RUNNING = 1;
    static final int POOL_CLEARING = 2;
    static final int POOL_TERMINATING = 3;
    static final int POOL_TERMINATED = 4;

    //worker state
    static final Object WORKER_IDLE = new Object();
    static final Object WORKER_WORKING = new Object();
    static final Object WORKER_TERMINATED = new Object();
}


