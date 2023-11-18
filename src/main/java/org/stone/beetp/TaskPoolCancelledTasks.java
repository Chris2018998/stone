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

import java.util.List;

/**
 * Cancelled tasks when execution terminated
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskPoolCancelledTasks {
    private final List<Task> taskList;

    private final List<TreeTask> treeTaskList;

    public TaskPoolCancelledTasks(List<Task> taskList, List<TreeTask> treeTaskList) {
        this.taskList = taskList;
        this.treeTaskList = treeTaskList;
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public List<TreeTask> getTreeTaskList() {
        return treeTaskList;
    }
}
