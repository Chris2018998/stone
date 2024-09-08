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
 * Statics definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public class PoolConstants {
    //pool state
    public static final int POOL_NEW = 0;
    public static final int POOL_STARTING = 1;
    public static final int POOL_RUNNING = 2;
    public static final int POOL_CLEARING = 3;
    public static final int POOL_TERMINATING = 4;
    public static final int POOL_TERMINATED = 5;

    //a state of worker is dead
    public static final int WORKER_INACTIVE = 0;
    //a state of worker in initializing
    public static final int WORKER_STARTING = 1;
    //a state of worker in working
    public static final int WORKER_RUNNING = 2;
    //a state of worker in waiting
    public static final int WORKER_WAITING = 3;

    //a task state that wait to be executed
    public static final Object TASK_WAITING = new Object();
    //a task state that a task is cancelled
    public static final Object TASK_CANCELLED = new Object();
    //a task state that task call is executed successful
    public static final Object TASK_SUCCEED = new Object();
    //a task state that task call is executed fail
    public static final Object TASK_FAILED = new Object();
}


