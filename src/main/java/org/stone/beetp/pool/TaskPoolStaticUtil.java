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
public class TaskPoolStaticUtil {
    //task state
    public static final int TASK_WAITING = 1;//waiting to be called
    public static final int TASK_CALLING = 2;//executing in worker thread
    public static final int TASK_CANCELLED = 3;
    public static final int TASK_RESULT = 4;
    public static final int TASK_EXCEPTION = 5;
    //pool state
    static final int POOL_READY = 0;
    static final int POOL_CLEARING = 1;
    static final int POOL_TERMINATING = 2;
    static final int POOL_TERMINATED = 3;
    //worker state
    static final Object WORKER_IDLE = new Object();
    static final Object WORKER_TERMINATED = new Object();

    //calculate next execution time(Nanoseconds)
    static long calculateNextRunTime(long startNanoTime, long delayNanoTime) {
        long nextRunTime = startNanoTime;
        if (delayNanoTime > 0) {//periodic
            while (nextRunTime <= System.nanoTime()) {
                nextRunTime = +delayNanoTime;
            }
        }
        return nextRunTime;
    }
}


