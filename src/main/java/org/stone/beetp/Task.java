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
 * A callable task interface.
 *
 * @param <T> is result type of task execution
 */

public interface Task<T> {

    /**
     * This method called in {@link TaskPool}
     *
     * @return result of task execution
     * @throws Exception when failed to be executed
     */
    T call() throws Exception;
}
