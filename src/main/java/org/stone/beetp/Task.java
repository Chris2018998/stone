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
 * Task Interface,which is similar to {@link java.util.concurrent.Callable} interface.
 * This interface defines a single method{@link #call},which returns a computed result or throws
 * an exception. Usually,task implementation instances are submitted to task pools(maybe call it thread executor),
 * and pooled worker threads will execute them by invoking their {@code call} methods.
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface Task<T> {

    /**
     * method call then return its computed result or throws out failed exception of computation
     *
     * @return a computed result
     * @throws Exception if executed failed in pool
     */
    T call() throws Exception;
}
