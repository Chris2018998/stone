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

import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;

import java.util.concurrent.TimeUnit;

/**
 * Task test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class MyTaskTest {

    public static void main(String[] args) throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(10));
        BeeTaskService service = new BeeTaskService(config);

        //submit a once task(only once execution)
        BeeTaskHandle handle = service.submit(new HelloTask());
        //submit a periodic task
        service.scheduleAtFixedRate(new TimeTask(), 0, 2, TimeUnit.SECONDS);
        //print once call result
        System.out.println("Result:" + handle.get());
    }
}
