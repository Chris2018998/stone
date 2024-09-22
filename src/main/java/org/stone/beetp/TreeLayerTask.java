
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
 * tree task interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TreeLayerTask<V> {

    /**
     * Gets subtasks of tree task
     *
     * @return an array of subtasks of tree task
     */
    TreeLayerTask<V>[] getSubTasks();

    /**
     * Join handles of subtasks to make a joined result
     *
     * @param subTaskHandles is a handle array of subtasks
     * @return a joined result of subtask handles
     * @throws Exception when join fail
     */
    V join(TaskHandle<V>[] subTaskHandles) throws Exception;
}
