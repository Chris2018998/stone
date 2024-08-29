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
 * An operator interface on joined task
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskJoinOperator<V> {

    /**
     * Attempt to split a task to some subtasks
     *
     * @param task to be split
     * @return an array of subtask split from given task
     */
    Task<V>[] split(Task<V> task);

    /**
     * Join handles of subtasks to make a joined result object
     *
     * @param subTaskHandles result array of subtasks
     * @return join result of subtask handles
     */
    V join(TaskHandle<V>[] subTaskHandles);
}
