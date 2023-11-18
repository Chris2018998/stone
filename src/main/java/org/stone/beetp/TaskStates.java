/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

/**
 * Task state definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskStates {

    //wait to be executed(initial state of tasks)
    public static final int TASK_WAITING = 0;

    //in executing state within a work thread
    public static final int TASK_EXECUTING = 1;

    //task done state,cancelled by users or by execution when it shutdown
    public static final int TASK_CANCELLED = 2;

    //task done state,a execution completed result can be taken out by <method>get<method> from a task
    public static final int TASK_CALL_RESULT = 3;

    //task done state,an exception occurred during task execution,can be thrown out when task <method>get<method> is called
    public static final int TASK_CALL_EXCEPTION = 4;


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
        return state == TASK_CALL_RESULT;
    }

    public static boolean isCallException(int state) {
        return state == TASK_CALL_EXCEPTION;
    }
}
