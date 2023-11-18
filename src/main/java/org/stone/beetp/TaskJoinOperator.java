/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
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
     * try to split a given task into some sub tasks,worker thread offers them to worker private queue.
     *
     * @param task target task
     * @return an array of sub tasks split from the given task,if cannot to be divided,then return a null array
     */
    Task<E>[] split(Task<E> task);

    /**
     * Join a sub task handles
     *
     * @param subTaskHandles result array of sub tasks
     * @return joined result
     */
    E join(TaskHandle<E>[] subTaskHandles);
}
