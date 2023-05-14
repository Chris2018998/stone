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
import java.util.concurrent.locks.LockSupport;
import java.util.Date;
import java.text.SimpleDateFormat;
/**
 * Task interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public class TimeTaskTest implements BeeTask {
    private String name;
    private SimpleDateFormat format;

    public TimeTaskTest(String name) {
        this.name = name;
        this.format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    }

    public static void main(String[] args) throws Exception {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(10));
        BeeTaskService service = new BeeTaskService(config);

        BeeTaskHandle handle1 = service.schedule(new TimeTaskTest("OneTime"), 0, TimeUnit.NANOSECONDS);
        BeeTaskHandle handle2 = service.scheduleAtFixedRate(new TimeTaskTest("FixedRate1"), 0, 2, TimeUnit.SECONDS);
        BeeTaskHandle handle3 = service.scheduleWithFixedDelay(new TimeTaskTest("FixedDelay2"), 0, 2, TimeUnit.SECONDS);
        BeeTaskHandle handle4 = service.scheduleAtFixedRate(new TimeTaskTest("FixedRate3"), 0, 2, TimeUnit.SECONDS);
        BeeTaskHandle handle5 = service.scheduleWithFixedDelay(new TimeTaskTest("FixedDelay4"), 0, 2, TimeUnit.SECONDS);
        LockSupport.park();
    }

    public String toString() {
        return name;
    }

    public Object call() throws Exception {
        long time= System.currentTimeMillis();
        System.out.println("<" + name + ">Current time:" + format.format(new Date()));
        return "Hello";
    }
}
