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
 * A Task interface,whose implementation instances
 *
 * @param <T> is a generic type of task execution task
 */

public interface Task<T> {

    /**
     * @return result of task execution
     * @throws Exception when failed to be executed
     */
    T call() throws Exception;
}
