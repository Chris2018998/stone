
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
 * task interface,which has treed type structure
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TreeLayerTask<V> {

    /**
     * Gets subtasks of this tree task
     *
     * @return an array of subtasks of tree task
     */
    TreeLayerTask<V>[] getSubTasks();

    /**
     * Join handles of subtasks to make a joined object
     *
     * @param subTaskHandles handle array of subtasks
     * @return execution value of method call
     * @throws Exception occurred in execution
     */
    V call(TaskHandle<V>[] subTaskHandles) throws Exception;
}
