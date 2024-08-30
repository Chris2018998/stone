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
 * A callable task interface
 *
 * @param <V> is result type of task execution
 * @author Chris Liao
 * @version 1.0
 */

public interface Task<V> {

    /**
     * Method to compute a result
     *
     * @return result of task execution
     * @throws Exception when fail
     */
    V call() throws Exception;
}
