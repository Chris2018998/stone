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

import java.util.concurrent.TimeUnit;

/**
 * Task interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public class HelloWorldTest implements BeeTask {

    private int seqNum;

    HelloWorldTest(int seqNum) {
        this.seqNum = seqNum;
    }

    public static void main(String[] args) throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(10));
        BeeTaskService service = new BeeTaskService(config);
        for (int i = 0; i < 100; i++) {
            BeeTaskHandle handle = service.submit(new HelloWorldTest(i));
            System.out.println(handle.get());
        }
    }

    public BeeTaskCallback getAspect() {
        return null;
    }

    public Object call() throws Exception {
        return "Hello World_" + seqNum;
    }
}
