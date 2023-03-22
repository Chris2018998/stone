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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * pool util
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolStaticCenter {
    public static final Logger CommonLog = LoggerFactory.getLogger(PoolStaticCenter.class);
    //task state
    static final int TASK_NEW = 0;
    static final int TASK_RUNNING = 1;
    static final int TASK_CANCELLED = 2;
    static final int TASK_EXCEPTIONAL = 3;
    static final int TASK_COMPLETED = 4;
    //pool state
    static final int POOL_NEW = 0;
    static final int POOL_STARTING = 1;
    static final int POOL_READY = 2;
    static final int POOL_TERMINATING = 3;
    static final int POOL_TERMINATED = 4;
    static final int POOL_CLEARING = 5;
    //worker state
    static final int WORKER_IDLE = 0;
    static final int WORKER_RUNNING = 1;
    static final int WORKER_TERMINATED = 2;


}
