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
 * A view object represent pool termination info contains some tasks cancelled list
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class TaskPoolTerminatedVo {
    private final List<Task> onceTaskList;
    private final List<Task> scheduledTaskList;
    private final List<Task> joinTaskList;
    private final List<TreeTask> treeTaskList;

    public TaskPoolTerminatedVo(List<Task> taskList, List<Task> scheduleList,
                                List<Task> joinTaskList, List<TreeTask> treeTaskList) {
        this.onceTaskList = taskList;
        this.scheduledTaskList = scheduleList;
        this.joinTaskList = joinTaskList;
        this.treeTaskList = treeTaskList;
    }

    public List<Task> getOnceTaskList() {
        return onceTaskList;
    }

    public List<Task> getScheduledTaskList() {
        return scheduledTaskList;
    }

    public List<Task> getJoinTaskList() {
        return joinTaskList;
    }

    public List<TreeTask> getTreeTaskList() {
        return treeTaskList;
    }
}
