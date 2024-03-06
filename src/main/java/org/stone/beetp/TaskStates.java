/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

/**
 * Task state definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskStates {

    //task waiting state(wait to be executed)
    public static final int TASK_WAITING = 0;

    //task executing state(processing in pool)
    public static final int TASK_EXECUTING = 1;

    //task cancelled state(abandon execution)
    public static final int TASK_CANCELLED = 2;

    //completion state of task execution,a result can be get when call get method of task handle
    public static final int TASK_EXEC_RESULT = 3;

    //failure state of task execution,a cause exception thrown when call get method of task handle
    public static final int TASK_EXEC_EXCEPTION = 4;

    public static boolean isWaiting(int state) {
        return state == TASK_WAITING;
    }

    public static boolean isExecuting(int state) {
        return state == TASK_EXECUTING;
    }

    public static boolean isDone(int state) {
        return state >= TASK_CANCELLED;
    }

    public static boolean isCancelled(int state) {
        return state == TASK_CANCELLED;
    }

    public static boolean isCallResult(int state) {
        return state == TASK_EXEC_RESULT;
    }

    public static boolean isCallException(int state) {
        return state == TASK_EXEC_EXCEPTION;
    }
}
