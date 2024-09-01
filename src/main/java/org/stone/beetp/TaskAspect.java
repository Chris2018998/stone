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
     * method to execute some computation before task call
     *
     * @param handle of a task
     */
    void beforeCall(TaskHandle<V> handle);

    /**
     * method to execute some computation after task Successful
     *
     * @param isSuccessful is true that task call successful
     * @param result       is result of call or an TaskExecutionException
     * @param handle       of a task
     */
    void afterCall(boolean isSuccessful, Object result, TaskHandle<V> handle);
}
