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

import java.util.concurrent.TimeUnit;

/**
 * Task test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class MyTaskTest {

    public static void main(String[] args) throws Exception {
        TaskServiceConfig config = new TaskServiceConfig();
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(10));
        TaskService service = new TaskService(config);

        //submit a once task(only once execution)
        TaskHandle handle = service.submit(new HelloTask());
        //submit a periodic task
        service.scheduleAtFixedRate(new TimeTask(), 0, 2, TimeUnit.SECONDS);
        //print once call result
        System.out.println("Result:" + handle.get());
    }
}
