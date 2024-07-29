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
 * Joining operation interface,be submitted to pool with its owner task,
 * the entire purpose is similar to {@link java.util.concurrent.ForkJoinTask} class.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskJoinOperator<E> {

    /**
     * try to split a given task into some subtasks,worker thread offers them to worker private queue.
     *
     * @param task target task
     * @return an array of subtasks split from the given task,if cannot to be divided,then return a null array
     */
    Task<E>[] split(Task<E> task);

    /**
     * Join subtask handles to result
     *
     * @param subTaskHandles result array of subtasks
     * @return joined result
     */
    E join(TaskHandle<E>[] subTaskHandles);
}
