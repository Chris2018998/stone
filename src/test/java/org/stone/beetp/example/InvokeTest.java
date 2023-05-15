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

import org.stone.beetp.BeeTask;
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;

import java.util.ArrayList;
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
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(10));
        BeeTaskService service = new BeeTaskService(config);

        List<BeeTask> taskList = new ArrayList<>(3);
        taskList.add(new HelloTask());
        taskList.add(new HelloTask());
        taskList.add(new HelloTask());
        BeeTaskHandle anyHandle = service.invokeAny(taskList);
        System.out.println("[Any]:" + anyHandle.get());

        List<BeeTask> taskList2 = new ArrayList<>(3);
        taskList2.add(new HelloTask());
        taskList2.add(new HelloTask());
        taskList2.add(new HelloTask());
        List<BeeTaskHandle> handleList = service.invokeAll(taskList2);
        for (BeeTaskHandle handle : handleList) {
            System.out.println("[One of all]:" + handle.get());
        }
    }
}
