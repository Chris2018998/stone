/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp.example;

import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * invoke demo
 *
 * @author Chris Liao
 * @version 1.0
 */
public class InvokeTest {
    public static void main(String[] args) throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(10));
        TaskService service = new TaskService(config);

        Collection<HelloTask> taskList = new ArrayList<>(3);
        taskList.add(new HelloTask());
        taskList.add(new HelloTask());
        taskList.add(new HelloTask());
        TaskHandle anyHandle = service.invokeAny(taskList);
        System.out.println("[Any]:" + anyHandle.get());

        List<HelloTask> taskList2 = new ArrayList<>(3);
        taskList2.add(new HelloTask());
        taskList2.add(new HelloTask());
        taskList2.add(new HelloTask());
        List<TaskHandle<Object>> handleList = service.invokeAll(taskList2);
        for (TaskHandle handle : handleList) {
            System.out.println("[One of all]:" + handle.get());
        }

        service.shutdown(false);
    }
}
