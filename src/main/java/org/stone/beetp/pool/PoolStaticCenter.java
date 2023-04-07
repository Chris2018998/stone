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
public class PoolStaticCenter {
    //task state
    static final int TASK_NEW = 0;
    static final int TASK_RUNNING = 1;
    static final int TASK_CANCELLED = 2;
    static final int TASK_EXCEPTIONAL = 3;
    static final int TASK_CALL_RESULT = 4;
    //pool state
    static final int POOL_READY = 0;
    static final int POOL_CLEARING = 1;
    static final int POOL_TERMINATING = 2;
    static final int POOL_TERMINATED = 3;
    //worker state
    static final int WORKER_IDLE = 0;
    static final int WORKER_RUNNING = 1;
    static final int WORKER_TERMINATED = 2;
}
