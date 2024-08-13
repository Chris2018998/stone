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
 * Call back interface of {@link Task}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskCallback {

    /**
     * method invoked before task execution
     *
     * @param handle of a task
     */
    void beforeCall(TaskHandle handle);

    /**
     * method invoked after task execution
     *
     * @param code   result code of execution
     * @param result of task execution
     * @param handle of task
     */
    void afterCall(int code, Object result, TaskHandle handle);
}
