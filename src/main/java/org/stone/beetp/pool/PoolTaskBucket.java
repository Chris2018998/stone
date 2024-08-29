 
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

/**
 * Pool task bucket interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface PoolTaskBucket {

    /**
     * put a task to this bucket
     *
     * @param taskHandle to be put
     */
    void put(PoolTaskHandle<?> taskHandle);

    /**
     * cancel a task from this bucket
     *
     * @param taskHandle            to be cancelled
     * @param mayInterruptIfRunning is true and task blocking in execution,then interrupt this blocking
     * @return true if success
     */
    boolean cancel(PoolTaskHandle<?> taskHandle, boolean mayInterruptIfRunning);
}
