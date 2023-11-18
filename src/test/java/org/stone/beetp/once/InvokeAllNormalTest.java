/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.once;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beetp.*;

import java.util.ArrayList;
import java.util.List;

public class InvokeAllNormalTest extends TestCase {
    public void test() throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        TaskService service = new TaskService(config);

        List<Task> taskList = new ArrayList();
        taskList.add(new HelloTask());
        taskList.add(new HelloTask());
        taskList.add(new ExceptionTask());

        List<TaskHandle> handleList = service.invokeAll(taskList);
        for (TaskHandle handle : handleList) {
            if (handle.getState() == TaskStates.TASK_CANCELLED)
                TestUtil.assertError("Exist one cancelled task in invoke all List");
        }
    }
}
