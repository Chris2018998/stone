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
 * Join Task operator
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskJoinOperator<E> {

    /**
     * try to split task into sub tasks
     *
     * @param task target task
     * @return array of sub tasks,return null when target task is at leaf
     */
    BeeTask<E>[] split(BeeTask<E> task);

    /**
     * joins result array of sub tasks to a valued object
     *
     * @param subTaskHandles result array of sub tasks
     * @return joined result
     */
    E join(BeeTaskHandle<E>[] subTaskHandles);
}
