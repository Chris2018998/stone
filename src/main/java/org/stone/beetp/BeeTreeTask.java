
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
 * Tree Task interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTreeTask<E> {

    /**
     * @return sub tasks of current task
     */
    BeeTreeTask<E>[] getSubTasks();

    /**
     * execute call with handle array of sub tasks
     *
     * @param subTaskHandles handle array of sub tasks
     * @return execution value of method call
     * @throws Exception occurred in execution
     */
    E call(BeeTaskHandle<E>[] subTaskHandles) throws Exception;
}
