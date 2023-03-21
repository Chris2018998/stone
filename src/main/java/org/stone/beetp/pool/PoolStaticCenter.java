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
    private static final int TASK_NEW = 0;
    private static final int TASK_RUNNING = 1;
    private static final int TASK_CANCELLED = 2;
    private static final int TASK_EXCEPTIONAL = 3;
    private static final int TASK_COMPLETED = 4;
    //pool state
    private static final int POOL_NEW = 0;
    private static final int POOL_STARTING = 1;
    private static final int POOL_READY = 2;
    private static final int POOL_TERMINATING = 3;
    private static final int POOL_TERMINATED = 4;
    private static final int POOL_CLEARING = 5;
    //worker state
    private static final int WORKER_IDLE = 0;
    private static final int WORKER_RUNNING = 1;
    private static final int WORKER_TERMINATED = 2;

}
