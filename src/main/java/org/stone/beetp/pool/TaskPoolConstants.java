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
public class TaskPoolConstants {
    public static final int TASK_CANCELLED = 2;
    public static final int TASK_CALL_RESULT = 3;
    public static final int TASK_CALL_EXCEPTION = 4;
    //task state
    static final int TASK_WAITING = 0;//waiting to be called
    static final int TASK_EXECUTING = 1;//executing in worker thread
    //pool state
    static final int POOL_RUNNING = 0;
    static final int POOL_CLEARING = 1;
    static final int POOL_TERMINATING = 2;
    static final int POOL_TERMINATED = 3;
    //worker state
    static final Object WORKER_IDLE = new Object();
    static final Object WORKER_WORKING = new Object();
    static final Object WORKER_TERMINATED = new Object();
}


