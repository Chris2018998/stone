
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
 * Tree task interface,which is a pre-splited join task
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TreeTask<E> {

    /**
     * return split sub tasks by manual
     *
     * @return sub tasks of current task
     */
    TreeTask<E>[] getSubTasks();

    /**
     * execute call with handle array of sub tasks
     *
     * @param subTaskHandles handle array of sub tasks
     * @return execution value of method call
     * @throws Exception occurred in execution
     */
    E call(TaskHandle<E>[] subTaskHandles) throws Exception;
}
