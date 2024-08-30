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
 * Aspect interface of task call
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskAspect<V> {

    /**
     * method to execute some computation before a task call
     *
     * @param handle of a task
     */
    void beforeCall(TaskHandle<V> handle);

    /**
     * method to execute some computation after a task call
     *
     * @param isSuccess is true that tall call is successful
     * @param result    is result of call or an TaskExecutionException
     * @param handle    of a task
     */
    void afterCall(boolean isSuccess, Object result, TaskHandle<V> handle);
}
