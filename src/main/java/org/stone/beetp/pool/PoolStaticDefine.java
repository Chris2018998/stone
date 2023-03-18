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
public class PoolStaticDefine {
    //task state
    static final int TASK_NEW = 0;//task default state
    static final int TASK_RUNNING = 1;
    static final int TASK_CANCELLED = 2;
    static final int TASK_EXCEPTION = 3;
    static final int TASK_COMPLETED = 4;

    //pool state
    static final int POOL_NEW = 0;//default state
    static final int POOL_STARTING = 1;
    static final int POOL_READY = 2;
    static final int POOL_CLOSED = 3;
    static final int POOL_CLEARING = 4;

    //worker state
    static final int WORKER_IDLE = 0;//default state of worker thread
    static final int WORKER_WORKING = 1;
    static final int WORKER_TERMINATED = 2;

}
