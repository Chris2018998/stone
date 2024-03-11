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
import java.util.concurrent.TimeUnit;

import static org.stone.beetp.TaskStates.TASK_CANCELLED;
import static org.stone.beetp.TaskStates.TASK_EXEC_EXCEPTION;

public class InvokeAllTimeoutTest extends TestCase {
    public void test() throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setMaxWorkerSize(1);
        TaskService service = new TaskService(config);

        List<Task> taskList = new ArrayList<>(3);
        taskList.add(new BlockTask());
        taskList.add(new HelloTask());
        taskList.add(new HelloTask());

        boolean existException = false;
        List<TaskHandle> handleList = service.invokeAll(taskList, 100, TimeUnit.MILLISECONDS);
        //System.out.println("handleList:" + handleList);

        for (TaskHandle handle : handleList) {
            int state = handle.getState();
            if (state == TASK_CANCELLED || state == TASK_EXEC_EXCEPTION) {
                existException = true;
                break;
            }
        }

        if (!existException) TestUtil.assertError("InvokeAll timeout test failed");
    }
}